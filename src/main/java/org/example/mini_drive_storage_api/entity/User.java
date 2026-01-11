package org.example.mini_drive_storage_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // Tránh dùng tên "user" vì trùng từ khóa SQL của Postgres
@Data // Lombok sinh Getter, Setter, toString...
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // Lưu hash BCrypt

    private String fullName;

    // Sau này sẽ thêm Role nếu cần
}
