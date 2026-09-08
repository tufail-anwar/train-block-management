package com.ps27.railway.repository;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Direction;
import com.ps27.railway.enums.TrainType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TrainScheduleRepositoryTest {

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private CorridorRepository corridorRepository;

    @Test
    void findByCorridorIdAndScheduleDateBetweenReturnsSchedules() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-S1", "Schedule Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        Corridor other = corridorRepository.save(
                new Corridor("COR-S2", "Other Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        Train train = trainRepository.save(new Train("TRN-S1", "Test Train", TrainType.PASSENGER, true));

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        trainScheduleRepository.save(schedule(train, corridor, from.plusDays(1)));
        trainScheduleRepository.save(schedule(train, corridor, from.minusDays(5)));
        trainScheduleRepository.save(schedule(train, other, from.plusDays(1)));

        List<TrainSchedule> result = trainScheduleRepository
                .findByCorridorIdAndScheduleDateBetween(corridor.getId(), from, to);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCorridor().getId()).isEqualTo(corridor.getId());
    }

    private TrainSchedule schedule(Train train, Corridor corridor, LocalDate date) {
        return new TrainSchedule(train, corridor, date, null,
                LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP);
    }
}
