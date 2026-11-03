package com.backlog.backend.model.request;


import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    String title;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 200, message = "Slug không được vượt quá 200 ký tự")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang"
    )
    String slug;

    @Size(max = 500, message = "Mô tả ngắn không được vượt quá 500 ký tự")
    String excerpt;

    @Size(max = 2048, message = "Đường dẫn ảnh không được vượt quá 2048 ký tự")
    String coverImageUrl;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    @Size(max = 100000, message = "Nội dung bài viết quá dài")
    String content;

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(
            regexp = "DRAFT|PUBLISHED",
            message = "Trạng thái chỉ được là DRAFT hoặc PUBLISHED"
    )
    String status;

    @NotNull(message = "Category không được để trống")
    Long categoryId;

    @NotNull(message = "Danh sách tag không được null")
    @Size(max = 10, message = "Một bài viết chỉ được có tối đa 10 tag")
    List<Long> tagIds;
}
