package com.rte_france.apogee.sea.server.services.utility;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.dao.timerange.TimerangeTypeRepository;
import com.rte_france.apogee.sea.server.model.timerange.EndType;
import com.rte_france.apogee.sea.server.model.timerange.StartType;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeFilterDate;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimerangeFilter {

    private TimerangeTypeRepository timerangeTypeRepository;

    @Autowired
    public TimerangeFilter(TimerangeTypeRepository timerangeTypeRepository) {
        this.timerangeTypeRepository = timerangeTypeRepository;
    }

    public TimerangeFilterDate getDateFilterByTimerange(String timerangeString) {

        Optional<TimerangeType> timerangeOptional = timerangeTypeRepository.findById(timerangeString);
        TimerangeType timerangeType = null;
        if (timerangeOptional.isPresent() && !"Tout".equals(timerangeString)) {
            timerangeType = timerangeOptional.get();
        }
        return getDateFilterByTimerange(new TimerangeFilterDate(), timerangeType);
    }

    /**
     * Filter list of network context by timerange type
     * @param timerangeString
     * @param networkContexts
     * @return
     */
    public List<NetworkContext> filterNetworkContextsByTimerangeType(String timerangeString, List<NetworkContext> networkContexts) {
        TimerangeFilterDate timerangeFilterDate = getDateFilterByTimerange(timerangeString);

        if (timerangeFilterDate.getStartDate() == null && timerangeFilterDate.getEndDate() == null) {
            return networkContexts;
        }

        if (timerangeFilterDate.getStartDate() == null) {
            return networkContexts.stream()
                    .filter(networkContext -> !networkContext.getNetworkDate().isAfter(timerangeFilterDate.getEndDate()))
                    .collect(Collectors.toList());
        }

        if (timerangeFilterDate.getEndDate() == null) {
            return networkContexts.stream()
                    .filter(networkContext -> !networkContext.getNetworkDate().isBefore(timerangeFilterDate.getStartDate()))
                    .collect(Collectors.toList());
        }

        return networkContexts.stream()
                .filter(networkContext -> !networkContext.getNetworkDate().isBefore(timerangeFilterDate.getStartDate()) &&  !networkContext.getNetworkDate().isAfter(timerangeFilterDate.getEndDate()))
                .collect(Collectors.toList());
    }


    /**
     * <p> Allows to set the start date and end date for the filter using the time range configuration object</p>
     *
     * @param timerange : The time range on which the filter is applied
     * @return : An object that contains the start date and end date, if the timerange is null then we return an object with null dates
     */
    private TimerangeFilterDate getDateFilterByTimerange(TimerangeFilterDate timerangeFilterDate, TimerangeType timerange) {
        Instant startDate = null;
        Instant endDate = null;
        if (timerange != null) {
            if (timerange.getStartType().equals(StartType.NOW)) {
                startDate = Instant.now().minusSeconds(timerange.getStartTimeMinutes() * 60L);
                timerangeFilterDate.setStartDate(startDate);
                if (timerange.getEndType().equals(EndType.HOURRELATIVE)) {
                    endDate = Instant.now().plusSeconds(timerange.getEndTimeHour() * 60 * 60L);
                    timerangeFilterDate.setEndDate(endDate);

                } else if (timerange.getEndType().equals(EndType.MIDNIGHT)) {
                    Instant curentDate = Instant.now();
                    endDate = curentDate.atZone(ZoneId.of(timerange.getTimeZone())).truncatedTo(ChronoUnit.DAYS).toInstant().plus(timerange.getEndTimeDay(), ChronoUnit.DAYS);
                    timerangeFilterDate.setEndDate(endDate);

                    long duration = ChronoUnit.HOURS.between(curentDate, endDate);
                    if (duration < timerange.getAlternateIfLessHoursThan()) {
                        getDateFilterByTimerange(timerangeFilterDate, timerange.getAlternateTimerange());
                    }

                }

            } else if (timerange.getStartType().equals(StartType.MIDNIGHT)) {
                startDate = Instant.now().atZone(ZoneId.of(timerange.getTimeZone())).truncatedTo(ChronoUnit.DAYS).toInstant().plus(timerange.getStartTimeDay(), ChronoUnit.DAYS);
                endDate = Instant.now().atZone(ZoneId.of(timerange.getTimeZone())).truncatedTo(ChronoUnit.DAYS).toInstant().plus(timerange.getEndTimeDay(), ChronoUnit.DAYS);
                timerangeFilterDate.setStartDate(startDate);
                timerangeFilterDate.setEndDate(endDate);

            }
        }

        return timerangeFilterDate;
    }

}
