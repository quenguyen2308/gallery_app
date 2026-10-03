# Đánh Giá Toàn Diện & Kế Hoạch Cải Thiện Màn Hình Chỉnh Sửa Ảnh (Editor Screen)

> **Ngày lập:** 02/10/2026  
> **Ứng dụng:** MyGallery  
> **Mục tiêu:** Tổng hợp các vấn đề kỹ thuật, lỗi crash, hạn chế UI/UX và lập kế hoạch nâng cấp màn hình biên tập ảnh (`EditorScreen`).

---

## 1. Lỗi Nghiêm Trọng Gây Văng Ứng Dụng (Critical Crash Bug)

### 💥 Lỗi xung đột Modifier.haze và Modifier.hazeChild
- **Vị trí tệp:** 
  - [`app/src/main/java/com/gallery/ui/navigation/GalleryApp.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/navigation/GalleryApp.kt) (dòng 164)
  - [`app/src/main/java/com/gallery/ui/editor/EditorScreen.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/EditorScreen.kt) (dòng 183 & 204)
  - [`app/src/main/java/com/gallery/ui/viewer/ImageViewerScreen.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/viewer/ImageViewerScreen.kt) (dòng 104)
- **Log lỗi:**
  ```text
  java.lang.IllegalArgumentException: Layout nodes using Modifier.haze and Modifier.hazeChild can not be descendants of each other
      at dev.chrisbanes.haze.HazeChildNode.draw(HazeChildNode.kt:277)
  ```
- **Nguyên nhân:**
  `NavHost` trong `GalleryApp.kt` được gán `Modifier.haze(hazeState)`. Khi người dùng mở `ImageViewerScreen` hoặc `EditorScreen` (vốn là các màn con bên trong `NavHost`), các màn hình này lại sử dụng `FloatingBottomBar` (bên trong gọi `Modifier.hazeChild(hazeState)`). Thư viện Chris Banes Haze cấm hoàn toàn quan hệ cha-con (ancestor-descendant) giữa `haze` và `hazeChild` trên cùng một `HazeState`.
- **Giải pháp:**
  - Không áp dụng `Modifier.haze(hazeState)` lên toàn bộ `NavHost`.
  - Chỉ áp dụng `haze(hazeState)` cục bộ cho nội dung cuộn ngầm (grid ảnh trong `PhotosScreen`, `AlbumsScreen`), hoặc cung cấp `HazeState` riêng biệt độc lập cho từng màn hình có nhu cầu làm mờ sương.

---

## 2. Vấn đề Bố cục & Trải nghiệm Người dùng (UI/UX Issues)

### 🛑 Thanh Floating Bottom Bar đè lấp toàn bộ công cụ điều khiển
- **Vị trí tệp:** [`app/src/main/java/com/gallery/ui/editor/EditorScreen.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/EditorScreen.kt)
- **Hiện tượng:**
  - `FloatingBottomBar` chuyển đổi giữa các tool (Crop, Cân chỉnh, Bộ lọc / AI) được neo ở giữa đáy màn hình (`BottomCenter`).
  - Trong khi đó, các sub-tool con cũng bố trí nút bấm và thanh trượt ở sát đáy màn hình:
    - **Crop & Rotate ([`CropRotateTool.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/basic/CropRotateTool.kt)):** Nút bấm **"Áp dụng" (Apply)**, các nút Xoay trái, Xoay phải, Lật ảnh nằm ngay dưới đáy $\rightarrow$ Bị thanh Bottom Bar đè lấp hoàn toàn, không thể bấm được.
    - **Cân chỉnh ([`AdjustTool.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/basic/AdjustTool.kt)):** Có 4 thanh trượt (Độ sáng, Tương phản, Bão hòa, Độ sắc nét) xếp chồng dọc $\rightarrow$ 2 thanh trượt dưới cùng (Bão hòa, Độ sắc nét) bị thanh Bottom Bar che khuất.
    - **Bộ lọc ([`FilterTool.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/basic/FilterTool.kt)):** Thanh trượt chỉnh cường độ bộ lọc (Intensity) nằm ở đáy $\rightarrow$ Bị đè lấp.
    - **Magic Eraser ([`MagicEraserTool.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/ai/MagicEraserTool.kt)):** Nút bấm **"Xóa vùng chọn"** và "Xóa nét vẽ" nằm ở đáy $\rightarrow$ Bị đè lấp.
- **Giải pháp:**
  - Tái cấu trúc layout màn hình `EditorScreen`:
    1. **Top Bar:** Back, Undo, Redo, Save.
    2. **Canvas Area (`weight(1f)`):** Vùng xem và thao tác trên ảnh.
    3. **Sub-tool Control Panel:** Khung điều khiển riêng của từng công cụ (Sliders, Filter list, Buttons...).
    4. **Bottom Tool Dock:** Thanh chọn công cụ cố định ở đáy, nằm bên dưới Sub-tool Panel, không để dạng floating tự do đè lên giao diện điều khiển.

### 📐 Lệch toạ độ nét vẽ Magic Eraser trên ảnh chụp dọc
- **Vị trí tệp:** [`app/src/main/java/com/gallery/ui/editor/ai/MagicEraserTool.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/ai/MagicEraserTool.kt) (dòng 61-75)
- **Hiện tượng:**
  Khung vẽ cọ dùng `.aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()).fillMaxWidth()`. Với ảnh chụp dọc (tỉ lệ 9:16 hoặc 3:4), việc dùng `.fillMaxWidth()` ép chiều cao bung ra vượt quá chiều cao container, dẫn đến biến dạng hoặc bị cắt xén. Khi gọi `buildMaskBitmap()`, tỷ lệ scale giữa màn hình vẽ và bitmap gốc bị lệch, khiến Gemini xóa sai vị trí vật thể.
- **Giải pháp:**
  Tính toán kích thước hiển thị thực tế của ảnh theo chế độ `Fit` trong viewport (letterbox/pillarbox) và đồng bộ ma trận toạ độ vẽ chính xác vào toạ độ pixel của ảnh gốc.

---

## 3. Các Tính Năng Cần Bổ Sung (Feature Enhancements)

1. **Nhấn giữ để so sánh Trước / Sau (Hold to Compare):**
   - Cho phép người dùng nhấn và giữ vào ảnh để tạm thời hiển thị ảnh gốc ban đầu (chưa áp dụng slider / filter / crop), thả tay ra để trở lại ảnh đang chỉnh.
2. **Hỗ trợ Zoom & Pan khi chỉnh sửa:**
   - Cho phép phóng to (pinch-to-zoom) và kéo rê ảnh (pan) để người dùng dễ dàng soi chi tiết khi làm nét ảnh hoặc khoanh cọ xóa các vật thể nhỏ trong Magic Eraser.
3. **Nút Undo từng nét vẽ cho Magic Eraser:**
   - Hiện tại chỉ có nút "Xóa tất cả nét vẽ". Cần thêm nút Undo nét vẽ gần nhất để không phải vẽ lại từ đầu nếu lỡ tay quẹt nhầm 1 nét.
4. **Cơ chế Hủy (Cancel) khi Gemini AI đang xử lý:**
   - Các tác vụ AI (Magic Eraser, Background Replace, Style Transfer) tốn 3–8 giây mạng. Cần có nút "Hủy" trên overlay xử lý để người dùng có thể ngắt tác vụ nếu lỡ bấm nhầm.
5. **Gợi ý trực quan cho Style Transfer:**
   - Thay các chip chữ đơn thuần ("Anime", "Tranh sơn dầu"...) bằng thẻ có hình ảnh preview mẫu cho từng phong cách.

---

## 4. Quản Lý Bộ Nhớ & Hiệu Năng (Performance & OOM Prevention)

- **Vị trí tệp:** [`app/src/main/java/com/gallery/ui/editor/EditorViewModel.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/ui/editor/EditorViewModel.kt)
- **Vấn đề:**
  - `history` lưu tối đa 6 `Bitmap` độ phân giải cao trong bộ nhớ RAM (`MAX_HISTORY = 6`).
  - Mỗi ảnh $2048 \times 2048$ tốn $\approx 16 \text{ MB}$ RAM $\rightarrow$ Chuỗi undo + preview bitmap có thể chiếm tới **120 MB – 160 MB** heap memory.
  - Nguy cơ gặp lỗi `OutOfMemoryError` trên thiết bị RAM thấp khi thao tác liên tục.
- **Giải pháp:**
  - Recycle các bitmap cũ khi bị đẩy ra khỏi stack undo.
  - Cân nhắc lưu snapshot tạm thời dạng file cache nén trên bộ nhớ trong (app internal storage) nếu cần mở rộng số bước undo.

---

## 5. Xử Lý Quyền Lưu Ảnh Scoped Storage (Android 10 - 14)

- **Vị trí tệp:** [`app/src/main/java/com/gallery/data/repository/MediaRepositoryImpl.kt`](file:///Users/que.nguyen/GIT/gallery_app/app/src/main/java/com/gallery/data/repository/MediaRepositoryImpl.kt) (dòng 355-365)
- **Vấn đề:**
  Khi chọn "Ghi đè" (Overwrite), nếu ảnh gốc không thuộc quyền sở hữu tạo bởi app trong session hiện tại, MediaStore sẽ từ chối ghi đè (ném ngoại lệ `RecoverableSecurityException`). Hiện tại repository đang âm thầm bắt ngoại lệ và tự động chuyển sang lưu thành ảnh copy mới mà không thông báo rõ ràng cho người dùng.
- **Giải pháp:**
  Hiển thị hộp thoại yêu cầu hệ thống xác nhận cấp quyền sửa tệp của MediaStore (`MediaStore.createWriteRequest`) để hỗ trợ ghi đè hoàn chỉnh trên Android 10+.

---

## 6. Kế Hoạch Triển Khai Tiếp Theo (Action Items)

| Giai đoạn | Nội dung công việc | Ưu tiên | Trạng thái |
| :--- | :--- | :---: | :---: |
| **Giai đoạn 1** | **Sửa lỗi crash Haze** giữa `NavHost` và `FloatingBottomBar` trong Viewer & Editor | 🔴 Khẩn cấp | ✅ Đã hoàn thành |
| **Giai đoạn 2** | **Tái cấu trúc bố cục `EditorScreen`**: Cố định Bottom Tool Dock, giải phóng không gian cho các Sub-tool sliders và nút "Áp dụng" | 🔴 Khẩn cấp | ✅ Đã hoàn thành |
| **Giai đoạn 3** | **Sửa lỗi tỷ lệ khung vẽ cọ Magic Eraser** cho ảnh dọc & thêm nút Undo nét vẽ | 🟡 Cao | ✅ Đã hoàn thành |
| **Giai đoạn 4** | **Thêm tính năng So sánh Trước / Sau** & Thay thư viện Cropper 8-handle touch | 🟡 Cao | ✅ Đã hoàn thành |
| **Giai đoạn 5** | **Bổ sung nút Cancel cho Gemini AI** & tối ưu hóa bộ nhớ Bitmap History | 🟢 Trung bình | ✅ Đã hoàn thành |
