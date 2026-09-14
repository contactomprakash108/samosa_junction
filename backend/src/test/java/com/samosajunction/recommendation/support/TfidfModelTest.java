package com.samosajunction.recommendation.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TfidfModelTest {

    @Test
    void rareTermsWeighMoreThanSharedTerms() {
        var model = TfidfModel.fit(List.of(
                List.of("tag:VEGETARIAN", "ing:paneer"),
                List.of("tag:VEGETARIAN", "ing:potato")
        ));
        var paneer = model.vector(List.of("tag:VEGETARIAN", "ing:paneer"));
        assertThat(paneer.get("ing:paneer")).isGreaterThan(paneer.get("tag:VEGETARIAN"));
    }

    @Test
    void emptyDocumentIsAnEmptyVector() {
        var model = TfidfModel.fit(List.of(List.of("ing:paneer")));
        assertThat(model.vector(List.of())).isEmpty();
    }
}
