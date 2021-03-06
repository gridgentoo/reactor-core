/*
 * Copyright (c) 2016-2022 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.test.StepVerifier;
import reactor.test.subscriber.AssertSubscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class FluxRepeatTest {

	@Test
	public void timesInvalid() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			Flux.never()
					.repeat(-1);
		});
	}

	@Test
	public void zeroRepeat() {
		StepVerifier.create(Flux.range(1, 10)
		                        .repeat(0))
		            .expectNextCount(10)
		            .verifyComplete();
	}

	@Test
	public void oneRepeat() {
		StepVerifier.create(Flux.range(1, 10)
		                        .repeat(1))
		            .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		            .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		            .verifyComplete();
	}

	@Test
	public void oneRepeatBackpressured() {
		StepVerifier.create(Flux.range(1, 10)
		                        .repeat(1),
				0)
		            .expectSubscription()
		            .expectNoEvent(Duration.ofMillis(100))
		            .thenRequest(2)
		            .expectNext(1, 2)
		            .thenRequest(3)
		            .expectNext(3, 4, 5)
		            .thenRequest(10)
		            .expectNext(6, 7, 8, 9, 10)
		            .expectNext(1, 2, 3, 4, 5)
		            .thenRequest(100)
		            .expectNext(6, 7, 8, 9, 10)
		            .verifyComplete();
	}

	@Test
	public void twoRepeat() {
		StepVerifier.create(Flux.range(1, 5)
		                        .repeat(2))
		            .expectNext(1, 2, 3, 4, 5)
		            .expectNext(1, 2, 3, 4, 5)
		            .expectNext(1, 2, 3, 4, 5)
		            .verifyComplete();
	}

	@Test
	public void twoRepeatNormal() {
		StepVerifier.create(Flux.just("test", "test2", "test3")
		                        .repeat(2)
		                        .count())
		            .expectNext(9L)
		            .expectComplete()
		            .verify();
	}

	@Test
	public void twoRepeatBackpressured() {
		StepVerifier.create(Flux.range(1, 5)
		    .repeat(2), 0)
		            .expectSubscription()
		            .expectNoEvent(Duration.ofMillis(100))
		            .thenRequest(2)
		            .expectNext(1, 2)
		            .thenRequest(4)
		            .expectNext(3, 4, 5).expectNext(1)
		            .thenRequest(2)
		            .expectNext(2, 3)
		            .thenRequest(10)
		            .expectNext(4, 5).expectNext(1, 2, 3, 4, 5)
		            .verifyComplete();
	}

	@Test
	public void repeatInfinite() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 2)
		    .repeat()
		    .take(9, false)
		    .subscribe(ts);

		ts.assertValues(1, 2, 1, 2, 1, 2, 1, 2, 1)
		  .assertComplete()
		  .assertNoError();
	}



	@Test
	public void twoRepeatNormalSupplier() {
		AtomicBoolean bool = new AtomicBoolean(true);

		StepVerifier.create(Flux.range(1, 4)
		                        .repeat(2, bool::get))
		            .expectNext(1, 2, 3, 4)
		            .expectNext(1, 2, 3, 4)
		            .expectNext(1, 2, 3, 4)
		            .then(() -> bool.set(false))
		            .verifyComplete();
	}

	@Test
	public void twoRepeatNormalSupplier2() {
		AtomicBoolean bool = new AtomicBoolean(false);

		StepVerifier.create(Flux.range(1, 4)
		                        .repeat(0, bool::get))
		            .expectNext(1, 2, 3, 4)
		            .verifyComplete();
	}

	@Test
	public void twoRepeatNormalSupplier3() {
		AtomicBoolean bool = new AtomicBoolean(true);

		StepVerifier.create(Flux.range(1, 4)
		                        .repeat(2, bool::get))
		            .expectNext(1, 2, 3, 4)
		            .expectNext(1, 2, 3, 4)
		            .expectNext(1, 2, 3, 4)
		            .verifyComplete();
	}

	@Test
	public void twoRepeatNormalSupplier4() {
		AtomicBoolean bool = new AtomicBoolean(false);

		StepVerifier.create(Flux.range(1, 4)
		                        .repeat(2, bool::get))
		            .expectNext(1, 2, 3, 4)
		            .verifyComplete();
	}

	@Test
	public void onLastAssemblyOnce() {
		AtomicInteger onAssemblyCounter = new AtomicInteger();
		String hookKey = UUID.randomUUID().toString();
		try {
			Hooks.onLastOperator(hookKey, publisher -> {
				onAssemblyCounter.incrementAndGet();
				return publisher;
			});
			Mono.just(1)
			    .repeat(1)
			    .blockLast();

			assertThat(onAssemblyCounter).hasValue(1);
		}
		finally {
			Hooks.resetOnLastOperator(hookKey);
		}
	}

	@Test
	public void scanOperator(){
		Flux<Integer> parent = Flux.just(1);
		FluxRepeat<Integer> test = new FluxRepeat<>(parent, 4);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(parent);
		assertThat(test.scan(Scannable.Attr.RUN_STYLE)).isSameAs(Scannable.Attr.RunStyle.SYNC);
	}

	@Test
	public void scanSubscriber(){
		Flux<Integer> source = Flux.just(1);
		CoreSubscriber<Integer> actual = new LambdaSubscriber<>(null, e -> {}, null, null);
		FluxRepeat.RepeatSubscriber<Integer> test = new FluxRepeat.RepeatSubscriber<>(source, actual, 3);

		Subscription parent = Operators.emptySubscription();
		test.onSubscribe(parent);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(parent);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(test.scan(Scannable.Attr.RUN_STYLE)).isSameAs(Scannable.Attr.RunStyle.SYNC);
	}
}
