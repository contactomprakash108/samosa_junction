package com.samosajunction.common.storage;

import com.samosajunction.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorTest {

    @Test
    void acceptsJpegWithMatchingMagic() {
        var validator = new ImageValidator(TestS3Properties.defaults());
        var file = new MockMultipartFile("file", "samosa.jpg", "image/jpeg", TestS3Properties.jpeg(64));

        var validated = validator.requireImage(file);

        assertThat(validated.contentType()).isEqualTo("image/jpeg");
        assertThat(validated.fileName()).isEqualTo("samosa.jpg");
        assertThat(validated.content()).hasSize(64);
    }

    @Test
    void rejectsDisallowedContentType() {
        var validator = new ImageValidator(TestS3Properties.defaults());
        var file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> validator.requireImage(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("JPEG, PNG, and WebP");
    }

    @Test
    void rejectsOversizedFile() {
        var validator = new ImageValidator(TestS3Properties.of(20, 5, "key"));
        var file = new MockMultipartFile("file", "big.jpg", "image/jpeg", TestS3Properties.jpeg(64));

        assertThatThrownBy(() -> validator.requireImage(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void rejectsMagicMismatch() {
        var validator = new ImageValidator(TestS3Properties.defaults());
        var file = new MockMultipartFile("file", "fake.png", "image/png", TestS3Properties.jpeg(32));

        assertThatThrownBy(() -> validator.requireImage(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void sanitizesPathAndOddCharacters() {
        assertThat(ImageValidator.sanitizeFileName("../../evil?.jpg", "image/jpeg"))
                .isEqualTo("evil_.jpg");
    }

}
