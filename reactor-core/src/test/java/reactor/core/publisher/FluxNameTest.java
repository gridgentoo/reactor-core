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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import reactor.core.Scannable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static org.assertj.core.api.Assertions.assertThat;

public class FluxNameTest {

	@Test
	public void scanOperator() throws Exception {
		Tuple2<String, String> tag1 = Tuples.of("foo", "oof");
		Tuple2<String, String> tag2 = Tuples.of("bar", "rab");
		List<Tuple2<String, String>> tags = Arrays.asList(tag1, tag2);

		Flux<Integer> source = Flux.range(1, 4).map(i -> i);
		FluxName<Integer> test = new FluxName<>(source, "foo", tags);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(source);
		assertThat(test.scan(Scannable.Attr.PREFETCH)).isEqualTo(-1);
		assertThat(test.scan(Scannable.Attr.RUN_STYLE)).isSameAs(Scannable.Attr.RunStyle.SYNC);

		assertThat(test.scan(Scannable.Attr.NAME)).isEqualTo("foo");

		final Stream<Tuple2<String, String>> scannedTags = test.scan(Scannable.Attr.TAGS);
		assertThat(scannedTags).isNotNull();
		assertThat(scannedTags).containsExactlyInAnyOrder(tag1, tag2);
	}

	@Test
	public void scanOperatorNullTags() throws Exception {
		Flux<Integer> parent = Flux.range(1, 4);
		FluxName<Integer> test = new FluxName<>(parent, "foo", null);

		assertThat(test.scan(Scannable.Attr.TAGS)).isNull();
	}

}
