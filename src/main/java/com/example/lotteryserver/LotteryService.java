package com.example.lotteryserver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryService {
    private static final int LOTTERY_NUMBER_COUNT = 6;
    private static final int LOTTERY_MIN_NUMBER = 1;
    private static final int LOTTERY_MAX_NUMBER = 45;
    private static final int RANDOM_LOTTERY_NUBMER_CREATION_RETRY_COUNT = 10;
    private final LotteryRepository lotteryRepository;

    public boolean register(Long lotteryRoundId, List<Integer> lotteryNumbers) {
        validate(lotteryNumbers);
        List<Integer> sortedLotteryNumbers = lotteryNumbers.stream().sorted().toList();
        String joinedLotteryNumbers = formatLotteryNumbersToString(sortedLotteryNumbers);
        Lottery lottery = new Lottery();
        lottery.setLotteryNumbers(joinedLotteryNumbers);
        lottery.setLotteryRoundId(lotteryRoundId);
        lotteryRepository.save(lottery);
        return true;
    }

    public List<Integer> createRandomLotteryNumbers(long currentRoundId) {
        int retryCount = RANDOM_LOTTERY_NUBMER_CREATION_RETRY_COUNT;
        List<Integer> randomLotteryNumbers;
        String joinedLotteryNumbers;
        do {
            randomLotteryNumbers = getDistinctRandomNumbers();
            validate(randomLotteryNumbers);
            joinedLotteryNumbers = formatLotteryNumbersToString(randomLotteryNumbers);
        } while(lotteryRepository.findByLotteryRoundIdAndLotteryNumbers(currentRoundId, joinedLotteryNumbers) != null && retryCount-- > 1);

        if (retryCount == 0) {
            throw new RandomLotteryNumberCreationFailedException();
        }

        return randomLotteryNumbers;
    }

    private void validate(List<Integer> lotteryNumbers) {
        if (lotteryNumbers.stream().distinct().count() != LOTTERY_NUMBER_COUNT) {
            throw new DuplicatedNumbersInLotteryNumberException();
        }
    }

    private List<Integer> getDistinctRandomNumbers() {
        Random random = new Random();
        Set<Integer> result = new HashSet<>();
        while(result.size() < LOTTERY_NUMBER_COUNT) {
            int number = random.nextInt(LOTTERY_MAX_NUMBER-1) + LOTTERY_MIN_NUMBER;
            result.add(number);
        }
        return result.stream().toList();
    }

    private String formatLotteryNumbersToString(List<Integer> lotteryNumbers) {
        Assert.notEmpty(lotteryNumbers, "lottery numbers should not be empty");
        Assert.isTrue(lotteryNumbers.size() == LOTTERY_NUMBER_COUNT, "lottery numbers should be fixed 6");

        return lotteryNumbers
                .stream()
                .map(s -> String.format("%02d", s))
                .collect(Collectors.joining("/"));
    }
}
