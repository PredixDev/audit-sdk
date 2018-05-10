package com.ge.predix.audit.sdk.util;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by 212582776 on 2/11/2018.
 */
public class StreamUtils {

    public static <T> Stream<List<T>> partitionOf(List<T> source, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length = " + length);
        }
        int size = source.size();
        if (size <= 0) {
            return Stream.empty();
        }
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

    private static <T> Spliterator<T> takeWhile(final Spliterator<T> spliterator, final Predicate<T> predicate, final boolean keepBreak) {
        return new Spliterators.AbstractSpliterator<T>(spliterator.estimateSize(), 0) {
            boolean stillGoing = true;
            @Override
            public boolean tryAdvance(final Consumer<? super T> consumer) {
                if (stillGoing) {
                    final boolean hadNext = spliterator.tryAdvance(elem -> {
                        if (predicate.test(elem)) {
                            consumer.accept(elem);
                        } else {
                            if (keepBreak) {
                                consumer.accept(elem);
                            }
                            stillGoing = false;
                        }
                    });
                    return hadNext && (stillGoing || keepBreak);
                }
                return false;
            }
        };
    }

     public static <T> Stream<T> takeWhile(final Stream<T> stream, final Predicate<T> predicate, final boolean keepBreak) {
        return StreamSupport.stream(takeWhile(stream.spliterator(), predicate, keepBreak), false);
    }
}

