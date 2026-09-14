package com.samosajunction.complaint.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.common.storage.ImageValidator;
import com.samosajunction.common.storage.ObjectStorage;
import com.samosajunction.common.storage.S3Properties;
import com.samosajunction.common.storage.TestS3Properties;
import com.samosajunction.complaint.entity.Complaint;
import com.samosajunction.complaint.entity.ComplaintCategory;
import com.samosajunction.complaint.entity.ComplaintImage;
import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.repository.ComplaintImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintImageServiceTest {

    @Mock
    private ComplaintService complaintService;

    @Mock
    private ComplaintImageRepository complaintImageRepository;

    @Mock
    private ObjectStorage objectStorage;

    private S3Properties properties;
    private ComplaintImageService complaintImageService;
    private UUID userId;
    private UUID complaintId;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        properties = TestS3Properties.defaults();
        complaintImageService = new ComplaintImageService(
                complaintService,
                complaintImageRepository,
                objectStorage,
                new ImageValidator(properties),
                properties
        );
        userId = UUID.randomUUID();
        complaintId = UUID.randomUUID();
        complaint = new Complaint(
                userId,
                UUID.randomUUID(),
                ComplaintCategory.DAMAGED,
                "Broken pastry",
                ComplaintPriority.HIGH,
                "img-1"
        );
        ReflectionTestUtils.setField(complaint, "id", complaintId);
    }

    @Test
    void uploadStoresObjectThenMetadata() {
        when(complaintService.requireVisible(userId, complaintId, false)).thenReturn(complaint);
        when(complaintImageRepository.countByComplaintId(complaintId)).thenReturn(0L);
        when(complaintImageRepository.saveAndFlush(any(ComplaintImage.class))).thenAnswer(invocation -> {
            ComplaintImage image = invocation.getArgument(0);
            ReflectionTestUtils.setField(image, "id", UUID.randomUUID());
            return image;
        });
        when(objectStorage.presignGet(anyString(), any(Duration.class)))
                .thenReturn(URI.create("http://localhost:8080/api/objects?key=x"));
        var file = jpegFile("samosa.jpg");

        var response = complaintImageService.upload(userId, complaintId, false, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).put(key.capture(), any(), eq("image/jpeg"));
        assertThat(key.getValue()).startsWith("complaints/" + complaintId + "/");
        assertThat(response.fileName()).isEqualTo("samosa.jpg");
        verify(objectStorage, never()).delete(anyString());
    }

    @Test
    void uploadHidesAnotherUsersComplaint() {
        when(complaintService.requireVisible(userId, complaintId, false))
                .thenThrow(new ResourceNotFoundException("Complaint not found"));

        assertThatThrownBy(() -> complaintImageService.upload(userId, complaintId, false, jpegFile("x.jpg")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Complaint not found");
        verify(objectStorage, never()).put(anyString(), any(), anyString());
    }

    @Test
    void uploadRejectsSixthImage() {
        when(complaintService.requireVisible(userId, complaintId, false)).thenReturn(complaint);
        when(complaintImageRepository.countByComplaintId(complaintId)).thenReturn(5L);

        assertThatThrownBy(() -> complaintImageService.upload(userId, complaintId, false, jpegFile("x.jpg")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 5");
        verify(objectStorage, never()).put(anyString(), any(), anyString());
    }

    @Test
    void uploadDeletesObjectWhenMetadataSaveFails() {
        when(complaintService.requireVisible(userId, complaintId, false)).thenReturn(complaint);
        when(complaintImageRepository.countByComplaintId(complaintId)).thenReturn(0L);
        when(complaintImageRepository.saveAndFlush(any(ComplaintImage.class)))
                .thenThrow(new RuntimeException("constraint"));

        assertThatThrownBy(() -> complaintImageService.upload(userId, complaintId, false, jpegFile("x.jpg")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("constraint");
        verify(objectStorage).put(anyString(), any(), eq("image/jpeg"));
        verify(objectStorage).delete(anyString());
    }

    @Test
    void uploadRejectsPlainText() {
        when(complaintService.requireVisible(userId, complaintId, false)).thenReturn(complaint);
        when(complaintImageRepository.countByComplaintId(complaintId)).thenReturn(0L);
        var file = new MockMultipartFile("file", "note.txt", "text/plain", "nope".getBytes());

        assertThatThrownBy(() -> complaintImageService.upload(userId, complaintId, false, file))
                .isInstanceOf(InvalidRequestException.class);
        verify(objectStorage, never()).put(anyString(), any(), anyString());
    }

    private static MockMultipartFile jpegFile(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", TestS3Properties.jpeg(64));
    }
}
