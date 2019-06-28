#!/bin/bash
set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

###########################################################################################

function version_get () {
  if [ ! -z "$maven_project" ]; then
    echo -en "getting project version (Maven) ..."
    current_version=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)
  elif [ ! -z "$node_project" ]; then
    echo -en "getting project version (Node) ..."
    current_version=$(node -pe "JSON.parse(require('fs').readFileSync('package.json')).version")
  fi

  if [[ "$current_version" =~ ([0-9]+)\.([0-9]+)\.([0-9]+)-SNAPSHOT ]]; then
      current_major=${BASH_REMATCH[1]}
      current_minor=${BASH_REMATCH[2]}
      current_patch=${BASH_REMATCH[3]}
      echo -e "\rProject Version is: ${GREEN}$current_version${NC} (major=${GREEN}$current_major${NC}, minor=${GREEN}$current_minor${NC}, patch=${GREEN}$current_patch${NC})"
  else
      echo -e "\r${RED}FATAL${NC}: Could not parse the version $current_version as x.y.z-SNAPSHOT"
      exit 1
  fi
}

###########################################################################################

package_json_version_change () {
  file=$1
  version=$2
  node -pe "const fs = require('fs');let data=JSON.parse(fs.readFileSync('$file')); data.version='$version'; fs.writeFileSync('$file', JSON.stringify(data, null, 2));"
  #add newline at end of file
  echo "" >> $file
}

###########################################################################################

function version_change () {
  version=$1
  message=$2
  tag=$3
  echo -e "\nMaven/Node project version change: version=${GREEN}$version${NC} message=\"${GREEN}$message${NC}\" tag=\"${GREEN}$tag${NC}\""

  # modify files with version info and add them for commit
  if [ ! -z "$maven_project" ]; then
    # call maven to change version
    mvn versions:set -DnewVersion="${version}" -q
    # cleanup the created 'versionsBackup' files
    find . -name pom.xml.versionsBackup -exec rm {} \;
    # stage the modified pom.xml for commit
    find . -name pom.xml -not -path "*/target/*" -exec git add {} \;
  elif [ ! -z "$node_project" ]; then
    package_json_version_change package.json ${version}
    package_json_version_change package-lock.json ${version}
    sed -i "s/sonar.projectVersion=.*/sonar.projectVersion=${version}/" sonar-project.properties
    git add package.json package-lock.json sonar-project.properties
  fi

  # now commit
  git commit -q -m "${message}"

  # if tag requested, do the tag
  if [ ! -z "$tag" ]; then
    git tag ${tag}
  fi
}

###########################################################################################

version_change_all () {
  version_change $freeze_version "freeze version $freeze_version" $git_tag
  if [ "$major_release" -eq "1" ]; then
    echo -e "Creating branch ${GREEN}$maintenance_branch${NC}"
    git checkout -b $maintenance_branch
    version_change $maintenance_version "preparing next version $maintenance_version [ci-skip]"
    #back to original branch - here should be develop
    git checkout -q $git_branch
  fi
  version_change $next_version "preparing next version $next_version [ci-skip]"
}

###########################################################################################

function git_check_branch () {
  # verify if local and remote branches are in sync
  branch=$1
  git checkout -q $branch
  sha1_local=$(git rev-parse $branch)
  sha1_remote=$(git rev-parse origin/$branch)
  echo -en "Verifying Git branch $branch (local:${GREEN}$sha1_local${NC}, remote:${GREEN}$sha1_remote${NC})"
  if [ "$sha1_local" != "$sha1_remote" ]; then
    echo -e " -> ${RED}FATAL${NC}: Git branch $branch is not in sync with remote"
    exit 1
  else
    echo -e " -> ${GREEN}OK${NC}!"
  fi
}

###########################################################################################

function display_summary () {
  echo
  echo -e "-> Will freeze  current     version: ${GREEN}$freeze_version${NC}  tag: ${GREEN}$git_tag${NC}"
  echo -e "-> Will prepare next        version: ${GREEN}$next_version${NC}"
  if [ "$major_release" -eq "1" ]; then
    echo -e "-> Will prepare maintenance version: ${GREEN}$maintenance_version${NC} in new branch ${GREEN}$maintenance_branch${NC}"
  fi
}

###########################################################################################

function confirm () {
  message=$1
  abort=$2
  echo
  read -p "$message (Y/N) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]
  then
    echo "Proceeding ..."
    answer=0
  else
    if [ ! -z "$abort" ]; then
      echo "Aborted"
      exit 1
    else
      echo "You answered 'no' ..."
      answer=1
    fi
  fi
}

###########################################################################################

function versions_consistency_checks () {
  if [ "$current_patch" -eq "0" ] && [ "$patch_release" -eq "1" ]; then
     echo -e "${RED}FATAL${NC}: maven project patch level is zero on maintenance release branch (Git branch is: ${GREEN}$git_branch${NC}, Project Version is: ${GREEN}$current_version${NC})"
     exit 1
  fi
  if [ "$current_patch" -ne "0" ] && [ "$major_release" -eq "1" ]; then
     echo -e "${RED}FATAL${NC}: maven project patch level is nonzero on develop branch (Git branch is: ${GREEN}$git_branch${NC}, Project Version is: ${GREEN}$current_version${NC})"
     exit 1
  fi
}

###########################################################################################

function identify_release_type () {
  echo -en "Git branch is: ${GREEN}$git_branch${NC}"
  if [ "$git_branch" == "develop" ]; then
    major_release=1
    echo -e " -> Will be doing a ${GREEN}MAJOR${NC} release"
    return
  fi
  if [[ "$git_branch" =~ release-([0-9]+)\.([0-9]+) ]]; then
    patch_release=1
    git_current_major=${BASH_REMATCH[1]}
    git_current_minor=${BASH_REMATCH[2]}
    echo -e " -> Will be doing a ${GREEN}PATCH${NC} release for $git_current_major.$git_current_minor"
    return
  fi
  echo -e " -> ${RED}FATAL${NC}: Could not identify release type. Git branch should be develop or release-x.y"
  exit 1
}

###########################################################################################

function increment_version () {
  next_minor=$current_minor
  next_patch=$current_patch
  if [ "$patch_release" -eq "1" ]; then
    next_patch=$((next_patch+1))
  fi
  if [ "$major_release" -eq "1" ]; then
    next_minor=$((next_minor+1))
    next_patch=0
    maintenance_minor=$current_minor
    maintenance_patch=$((next_patch+1)) #should be 1
    maintenance_version=${current_major}.${maintenance_minor}.${maintenance_patch}-SNAPSHOT
    maintenance_branch=release-${current_major}.${maintenance_minor}
  fi

  freeze_version=${current_major}.${current_minor}.${current_patch}
  next_version=${current_major}.${next_minor}.${next_patch}-SNAPSHOT

  if [ ! -z "$maven_project" ]; then
    git_tag=apogee-sea-backend-$freeze_version
  elif [ ! -z "$node_project" ]; then
    git_tag=apogee-sea-front-$freeze_version
  fi
}

###########################################################################################

function final_push() {
  gitpushcommand="git push origin master $git_branch $git_tag $maintenance_branch"
  echo
  echo -e "About to push this: ${GREEN}$gitpushcommand${NC}"

  confirm "Do you want to execute this git push ?"
  if [ "$answer" -eq "0" ]; then
    eval $gitpushcommand
  else
    echo -e "Here some useful command to get things back in order (${RED}use at your own risks !${NC})"
    echo "git checkout master && git reset --hard @{u}"
    echo "git checkout $git_branch && git reset --hard @{u}"
    echo "git tag -d $git_tag"
    if [ "$major_release" -eq "1" ]; then
      echo "git branch -D $maintenance_branch"
    fi
  fi
}

###########################################################################################

# git infos
major_release=0
patch_release=0
git_branch=$(git branch | grep \* | cut -d ' ' -f2)

if [ -f "pom.xml" ]; then
  echo "MAVEN"
  maven_project=1
elif [ -f "package.json" ]; then
  echo "NODE"
  node_project=1
else
  echo -e "\r${RED}FATAL${NC}: neither pom.xml nor package.json found"
fi

# check if we are doing a patch or feature release
identify_release_type

#check dirty status (do not bother untracked files)
if [[ $(git diff --shortstat 2> /dev/null | tail -n1) != "" ]]; then
  echo -e "\r${RED}FATAL${NC}: there are modified files"
  exit 1
fi

# git sanity checks. verify local is in sync with remote for master branch and for current branch (either develop or release-x.y)
git fetch
git_check_branch master
git_check_branch $git_branch


# get version from maven or node
version_get

# some consistency checks in versions:
# - patch release should have last digit > 0
# - feature release should have last digit = 0
versions_consistency_checks

# now increment
increment_version

# display actions summary and ask confirmation (1: abort if no confirmation)
display_summary
confirm "Are you sure?" 1

#do the real stuff: version changes, commits etc ...
version_change_all

#merge into master
echo -e "Merging tag ${GREEN}$git_tag${NC} into ${GREEN}master${NC}"
git checkout master
git merge $git_tag --strategy-option theirs -m "Merge tag '$git_tag'"

#back to original branch - develop or release-x.y
git checkout -q $git_branch

#push all relevant branches and tag
final_push

echo Done !
