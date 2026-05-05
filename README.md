
🌌 Aura Flow (POMODORO2) - Productivity & Garden System
Aura Flow là một ứng dụng quản lý năng suất đột phá, kết hợp giữa phương pháp đếm giờ Pomodoro khoa học và cơ chế Gamification (trò chơi hóa). Ứng dụng mang phong cách thiết kế Cyberpunk hiện đại với tông màu tối và hiệu ứng Neon (Hồng Aura, Xanh Mint), giúp người dùng duy trì sự tập trung một cách thú vị và kỷ luật.

🚀 Tính năng nổi bật
⏱️ Timer Engine Đa năng: Hỗ trợ linh hoạt các chế độ Pomodoro (25/5), Countdown (đếm ngược) và Stopwatch (đếm tiến).

🪴 Garden System (Gamification): Tập trung thành công để nhận Drops (giọt nước) giúp cây trong vườn phát triển. Dừng timer giữa chừng sẽ bị trừ tiền và làm cây chết, tạo động lực duy trì kỷ luật.

📱 Whitelist (Chống xao nhãng): Cho phép chọn danh sách ứng dụng "được phép" mở khi đang đếm giờ, hạn chế các ứng dụng gây xao nhãng khác.

🤖 Aura AI: Tích hợp trí tuệ nhân tạo để phân tích mục tiêu (Task) lớn thành các bước nhỏ và cung cấp châm ngôn động lực hàng ngày.

📟 Home Screen Widget: Sử dụng Jetpack Glance để điều khiển trình đếm giờ trực tiếp ngay từ màn hình chính.

🛠️ Bản đồ Công nghệ (Tech Stack)
Dự án áp dụng bộ công nghệ Android hiện đại nhất (2024-2025):

UI Framework: Jetpack Compose (Material 3) kết hợp Custom Canvas để vẽ hiệu ứng Cyberpunk.

Data Persistence: Room Database (lưu Task/Lịch sử) và DataStore Preferences (lưu cài đặt/Drops).

Background Processing: Foreground Service giúp đếm giờ chính xác ngay cả khi tắt ứng dụng.

Cloud & Auth: Firebase SDK (Authentication & Google Sign-In).

Networking: Retrofit + Gson để kết nối API châm ngôn và thời tiết.

Architecture: MVVM kết hợp Kotlin Coroutines & Flow để xử lý dữ liệu thời gian thực.

📸 Giao diện ứng dụng:
<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/1d55f9a9-2c99-453d-90fd-f9594bd0625a" />

<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/72164bad-beba-4e99-8397-401beefdda66" />


📋 Hướng dẫn cài đặt
Clone dự án:

Bash
git clone https://github.com/aknamngu/Pomodoro-App-Kotlin.git
Cấu hình Firebase: Đảm bảo bạn đã thêm file google-services.json vào thư mục /app của dự án.

Build dự án: Sử dụng Android Studio (phiên bản mới nhất), đồng bộ Gradle và nhấn Run trên thiết bị Android (API 24+).
