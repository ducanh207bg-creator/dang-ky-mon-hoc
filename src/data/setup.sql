CREATE TABLE IF NOT EXISTS students (id TEXT PRIMARY KEY, name TEXT NOT NULL, class_name TEXT NOT NULL, major TEXT NOT NULL, max_credits INTEGER NOT NULL);
        CREATE TABLE IF NOT EXISTS courses (id TEXT PRIMARY KEY, name TEXT NOT NULL, credits INTEGER NOT NULL, lecturer TEXT NOT NULL, capacity INTEGER NOT NULL, day INTEGER NOT NULL, start TEXT NOT NULL, end TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS registrations (student_id TEXT NOT NULL REFERENCES students(id), course_id TEXT NOT NULL REFERENCES courses(id), PRIMARY KEY(student_id, course_id));
INSERT INTO students VALUES
('SV01','Trần Văn Đức Anh','CNTT01','Công nghệ thông tin',18),
('SV02','Nguyễn Văn Long','CNTT01','Công nghệ thông tin',18),
('SV03','Nguyễn Tất Cương','CNTT01','Công nghệ thông tin',18),
('SV04','Nguyễn Quang Minh','CNTT01','Công nghệ thông tin',18)
ON CONFLICT(id) DO UPDATE SET name = excluded.name, max_credits = excluded.max_credits;
CREATE TABLE IF NOT EXISTS credentials (student_id TEXT PRIMARY KEY REFERENCES students(id), salt TEXT NOT NULL, hash TEXT NOT NULL);
-- Mật khẩu mẫu của cả 4 tài khoản: demo1234 (chỉ dùng để demo)
INSERT OR IGNORE INTO credentials VALUES
('SV01','iQ4PUlk8U4MWQYO5VYspQA==','oCAyTvoNntUpyHTrUEw5SEufMCJ3aWVHC6L9SJZSa6U='),
('SV02','YuuYfAIqybYBKV6O7LmYpg==','flgxv9WZSfrHnVIOG3jk0pjW3ZXzuGS/WavqT59ZBc8='),
('SV03','cJJ3jkNxvW69nCuFDs71Rg==','CoTHkI7UQfcV52D2TVttF6BTbcXgbHBlI4UYeOBWYF4='),
('SV04','KXIaPhUonm+LKyYqb6E0Ng==','RKcs788FEwzp2DMPR8SdJ7djP2EPVA8NHvjYmNzdk7w=');
INSERT INTO courses VALUES
('IT101','Lập trình Java',3,'Nguyễn An',20,2,'09:30','12:10'),
('IT102','Cơ sở dữ liệu',3,'Trần Bình',20,2,'09:30','12:10'),
('MA101','Toán rời rạc',3,'Lê Chi',20,3,'09:30','12:10'),
('EN101','Tiếng Anh',2,'Phạm Dung',1,4,'06:45','09:25'),
('IT103','Mạng máy tính',3,'Vũ Hà',20,5,'09:30','12:10'),
('IT104','Trí tuệ nhân tạo',3,'Phạm Văn Dũng',20,4,'13:00','15:40'),
('IT105','Cấu trúc dữ liệu và giải thuật',3,'Hoàng Mai',20,6,'13:00','15:40'),
('IT106','Hệ điều hành',3,'Đỗ Quân',20,2,'13:00','15:40'),
('IT107','Công nghệ phần mềm',3,'Bùi Lan',20,3,'13:00','15:40'),
('MA102','Xác suất thống kê',3,'Ngô Hải',20,6,'15:45','18:25'),
('PH101','Vật lý đại cương',2,'Đặng Sơn',20,8,'09:30','12:10')
ON CONFLICT(id) DO UPDATE SET day = excluded.day, start = excluded.start, end = excluded.end;
-- Phòng học để ở bảng riêng nên cơ sở dữ liệu cũ không cần sửa bảng courses
CREATE TABLE IF NOT EXISTS course_rooms (course_id TEXT PRIMARY KEY REFERENCES courses(id), room TEXT NOT NULL);
INSERT OR IGNORE INTO course_rooms VALUES
('IT101','P.301'),
('IT102','P.201'),
('MA101','P.202'),
('EN101','P.105'),
('IT103','P.401'),
('IT104','P.402'),
('IT105','P.302'),
('IT106','P.303'),
('IT107','P.304'),
('MA102','P.203'),
('PH101','P.106');
