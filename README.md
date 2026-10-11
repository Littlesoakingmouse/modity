# Modity

Modity là ứng dụng Android quản lý công việc và thói quen cá nhân: lên lịch task (một lần hoặc lặp lại), gom nhóm, theo dõi trạng thái từng lần thực hiện, tập trung theo phiên (Focus Mode) và xem thống kê hiệu suất. Giao diện theo phong cách tối giản (minimalism), hỗ trợ Sáng/Tối và tiếng Anh/tiếng Việt.

> Module **AI Habit Analyst** (phân tích và hỏi đáp bằng AI) sẽ được xây dựng sau. Dữ liệu cần cho AI (lịch sử trạng thái, phiên tập trung) đã được lưu sẵn.

## Công nghệ

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Kotlin |
| Giao diện | XML layouts, Android Views, ViewBinding, Material Components 3 (không dùng Compose) |
| Danh sách | RecyclerView + Adapter/ViewHolder, MaterialCardView |
| Cơ sở dữ liệu | SQLite qua Room |
| Cài đặt người dùng | SharedPreferences |
| Điều hướng | Intent giữa các Activity, truyền dữ liệu qua extras |
| Thông báo | AlarmManager + NotificationCompat |
| Min / Target SDK | 26 / 37 |

## Tính năng

### 1. Quản lý task
- Tạo task với: tên, mô tả, màu, biểu tượng, nhóm (không bắt buộc), ngày và giờ, hạn hoàn thành, mức ưu tiên, nhắc nhở, yêu cầu tập trung (kèm thời lượng 15/25/45/60/90 phút).
- Sửa và xóa task. Với task lặp lại, ứng dụng hỏi áp dụng cho **chỉ lần này** hay **toàn bộ lịch lặp**.
- Trạng thái của từng lần thực hiện:
  - **Pending**: chưa hoàn thành, còn trong hạn.
  - **Completed**: đã hoàn thành.
  - **Missed**: đã qua hạn mà chưa hoàn thành (tính tự động).
  - **Skipped**: người dùng chủ động bỏ qua.
- Tìm kiếm theo tên/mô tả; lọc theo nhóm, trạng thái, loại task (một lần/lặp lại) và ngày; sắp xếp theo thời gian, hạn, mức ưu tiên hoặc tên.

### 2. Nhóm task
- Tạo, sửa nhóm (tên, mô tả, màu, biểu tượng); kéo thả để đổi thứ tự hiển thị.
- Khi xóa nhóm, chọn **giữ lại task** (chuyển thành task không thuộc nhóm) hoặc **xóa luôn task**.
- Mỗi nhóm hiển thị số task, số task đã xong và phần trăm tiến độ trong ngày.
- Task không thuộc nhóm nào vẫn hiển thị trên màn hình chính như một nhóm riêng.

### 3. Lịch trình và task lặp lại
- Task một lần, lặp hằng ngày, hoặc lặp theo các thứ trong tuần.
- Ngày bắt đầu, ngày kết thúc hoặc lặp vô thời hạn.
- Mỗi lần xuất hiện có trạng thái riêng; hoàn thành thứ 2 và bỏ qua thứ 4 được lưu thành hai kết quả độc lập.
- Màn hình Lịch xem theo **Ngày / Tuần / Tháng**.

### 4. Kiểu hiển thị trang chủ
- **Card**: mỗi nhóm là một thẻ có thanh tiến độ và danh sách task.
- **Grid**: lưới 2 cột, truy cập nhanh.
- **Timeline**: task theo dòng thời gian, tự hiển thị các khoảng **thời gian trống** (từ 1 giờ trở lên).
- Lựa chọn được lưu lại. Đổi kiểu hiển thị không làm thay đổi dữ liệu.

### 5. Focus Mode
- Bắt đầu phiên từ tab Focus hoặc từ chi tiết task.
- Giao diện toàn màn hình tối giản: đồng hồ đếm ngược, tạm dừng/tiếp tục, kết thúc; giữ màn hình luôn sáng.
- Lưu mỗi phiên: thời điểm bắt đầu/kết thúc, thời lượng dự kiến, thời lượng thực tế, số lần tạm dừng, kết quả (hoàn thành / dừng sớm).
- Tùy chọn bật **Không làm phiền** trong phiên (cần người dùng cấp quyền). Ứng dụng không khóa điện thoại.

### 6. Thống kê
- Tổng quan: số task đã lên lịch, hoàn thành, bỏ lỡ, bỏ qua, tỷ lệ hoàn thành, tổng thời gian tập trung.
- Biểu đồ cột (số task hoàn thành mỗi ngày) và biểu đồ đường (xu hướng tỷ lệ hoàn thành), vẽ bằng custom View, không dùng thư viện ngoài.
- Thống kê theo nhóm, chuỗi thói quen (streak hiện tại và dài nhất), lịch sử theo ngày.
- Phân tích phiên tập trung: số phiên hoàn thành, thời lượng trung bình, tỷ lệ bị gián đoạn.
- Lọc theo Hôm nay / 7 ngày / 30 ngày và theo nhóm.

**Quy tắc streak:** chỉ tính những ngày task được lên lịch. Hoàn thành thì cộng chuỗi, bỏ lỡ thì reset; bỏ qua và chưa đến hạn thì không ảnh hưởng.

### 7. Cài đặt (SharedPreferences)
- Giao diện Sáng / Tối / Theo hệ thống.
- Ngôn ngữ: Theo hệ thống / English / Tiếng Việt.
- Kiểu hiển thị trang chủ, thời lượng Focus mặc định.
- Bật/tắt nhắc nhở; cấp quyền nhắc đúng giờ (exact alarm) trên Android 12 trở lên.
- Quản lý nhóm, thêm dữ liệu mẫu, xóa toàn bộ dữ liệu.

### Nhắc nhở và quyền
- Mỗi task có nhắc nhở được đặt một alarm cho lần xuất hiện kế tiếp; khi alarm kích hoạt, ứng dụng hiện thông báo (có nút **Hoàn thành**) rồi đặt lần nhắc tiếp theo.
- Alarm được đặt lại sau khi khởi động lại máy hoặc cập nhật ứng dụng.
- Quyền runtime: `POST_NOTIFICATIONS` (Android 13+) được xin khi bật nhắc nhở. Ngoài ra ứng dụng dùng `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_NOTIFICATION_POLICY`.

## Cấu trúc mã nguồn

```
app/src/main/java/com/example/modity/
├── ModityApp.kt               # Application: áp dụng theme, tạo notification channel
├── data/
│   ├── AppDatabase.kt         # Room database
│   ├── Daos.kt                # DAO cho task, nhóm, lần xuất hiện, phiên tập trung
│   ├── ModityRepository.kt    # Truy cập dữ liệu + tính trạng thái từng lần xuất hiện
│   ├── Stats.kt               # Tính thống kê, streak, phân tích focus
│   ├── UserPreferences.kt     # SharedPreferences
│   └── SampleData.kt          # Dữ liệu mẫu để demo
├── model/                     # Room entity + model: Task, TaskGroup, TaskOccurrence, FocusSession, DayTask…
├── notification/              # ReminderScheduler, ReminderReceiver, BootReceiver
├── ui/
│   ├── home/                  # Trang chủ: Card / Grid / Timeline
│   ├── task/                  # Tạo/sửa task, chi tiết task, tìm kiếm
│   ├── group/                 # Chi tiết nhóm, sửa nhóm, quản lý và sắp xếp nhóm
│   ├── calendar/              # Lịch Ngày / Tuần / Tháng
│   ├── focus/                 # Thiết lập phiên và đồng hồ tập trung
│   ├── statistics/            # Thống kê + biểu đồ custom View
│   ├── settings/              # Cài đặt
│   └── common/                # Thành phần dùng chung: dòng task, menu thao tác, bottom nav, bộ chọn màu/biểu tượng
└── utils/                     # Định dạng ngày giờ, xử lý insets
```

Tài nguyên: `res/layout` (XML), `res/values` (màu, kích thước, style, chuỗi tiếng Anh), `res/values-vi` (chuỗi tiếng Việt), `res/values-night` (màu chế độ tối), `res/drawable` (icon vector).

## Mô hình dữ liệu

| Bảng | Nội dung |
|---|---|
| `task_groups` | Nhóm: tên, mô tả, màu, biểu tượng, thứ tự |
| `tasks` | Task: thông tin, lịch lặp (`repeat_type`, `repeat_days` dạng bitmask), ngày bắt đầu/kết thúc, giờ, hạn, ưu tiên, nhắc nhở, focus |
| `task_occurrences` | Kết quả của một lần xuất hiện (`task_id` + `date`): COMPLETED, SKIPPED hoặc REMOVED (lần đó đã bị xóa/tách riêng) |
| `focus_sessions` | Phiên tập trung: task, thời gian, thời lượng dự kiến/thực tế, số lần tạm dừng, kết quả |

Trạng thái **Pending** và **Missed** không lưu vào bảng mà được tính từ lịch và hạn của task. Nhờ vậy trạng thái luôn đúng mà không cần tác vụ chạy nền.

## Chạy dự án

1. Mở thư mục dự án bằng Android Studio (JDK 17+).
2. Sync Gradle rồi chạy cấu hình `app` trên máy ảo hoặc thiết bị Android 8.0 (API 26) trở lên.

Dùng dòng lệnh:

```bash
./gradlew :app:assembleDebug      # build APK debug
./gradlew :app:compileDebugKotlin # kiểm tra biên dịch nhanh
./gradlew :app:lintDebug          # lint
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

**Dữ liệu demo:** vào *Cài đặt → Thêm dữ liệu mẫu* để tạo sẵn nhóm, task, 3 tuần lịch sử và các phiên tập trung.

> Database đang dùng `fallbackToDestructiveMigration()` trong giai đoạn phát triển: khi đổi schema, dữ liệu cũ trên máy sẽ bị xóa.

## Hướng phát triển
- Module AI Habit Analyst: phân tích hiệu suất, báo cáo tuần/tháng, hỏi đáp dựa trên dữ liệu, đề xuất điều chỉnh lịch (người dùng xác nhận trước khi áp dụng), lưu lịch sử báo cáo.
- Viết Room Migration thay cho destructive migration trước khi phát hành.
