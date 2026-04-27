# TÀI LIỆU QUY ƯỚC GIAO TIẾP JSON (CLIENT - SERVER)

Hệ thống giao tiếp qua Socket sử dụng chuỗi JSON. Dưới đây là 3 cấu trúc chuẩn bắt buộc mọi Request (yêu cầu) và Response (trả về) phải tuân theo.

## 1. Cấu trúc REQUEST (Client gửi lên Server)
Mọi yêu cầu từ Client gửi lên bắt buộc phải có `action` để định tuyến và `data` chứa dữ liệu chi tiết.

```json
{
  "action": "TÊN_HÀNH_ĐỘNG",
  "data": {
    "truongDuLieu1": "giaTri1",
    "truongDuLieu2": "giaTri2"
  }
}
```
Ví dụ thực tế (Client gửi yêu cầu Đăng nhập): 
```json
{
  "action": "LOGIN",
  "data": {
    "username": "haianh_bidder",
    "password": "password123"
  }
}
```
## 2. Cấu trúc RESPONSE - THÀNH CÔNG (Server trả về Client)
Khi Server xử lý thành công, status bắt buộc là "SUCCESS". Tham số data sẽ chứa đối tượng (Object) hoặc danh sách (Array) mà Client cần.
```json
{
  "status": "SUCCESS",
  "message": "Thông báo thành công để Frontend hiển thị",
  "data": {
    "truongDuLieu1": "giaTri1"
  }
}
```
Ví dụ thực tế (Đăng nhập thành công, trả về thông tin User):
```json
{
  "status": "SUCCESS",
  "message": "Đăng nhập thành công!",
  "data": {
    "userId": 1,
    "role": "BIDDER",
    "fullName": "Hải Anh"
  }
}
```
## 3. Cấu trúc RESPONSE - THẤT BẠI (Server trả về Client)
```json
{
  "status": "ERROR",
  "message": "Sai tài khoản hoặc mật khẩu. Vui lòng thử lại!",
  "data": null
}
```