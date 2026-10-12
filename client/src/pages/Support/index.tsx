import { Box, Typography, Stack, Paper, Button } from '@mui/material'
import { Link } from 'react-router'
export default function Support() {
  return <Box sx={{ maxWidth: 800, mx: 'auto', p: 4 }}><Paper sx={{ p: 4 }}><Stack spacing={3}>
    <Typography variant="h4">Hướng dẫn Homura Shop demo</Typography>
    <Typography>Đây là ứng dụng trình diễn local. Dùng thông tin giả lập, không chuyển tiền và không nhập dữ liệu ngân hàng thật.</Typography>
    <Typography variant="h6" id="help">Mua hàng và hỗ trợ</Typography>
    <Typography>Thêm địa chỉ trong tài khoản, chọn biến thể và đưa vào giỏ. Ship được miễn khi tổng sau giảm từ 500.000đ. Coupon mẫu: DEMO10. COD, VNPay và MoMo dùng để mô phỏng các bước mua hàng.</Typography>
    <Typography>Thanh toán lỗi hoặc bỏ dở: mở Đơn mua để thử lại hoặc hủy đơn chưa trả tiền. Sau 15 phút hãy tạo phiên thanh toán mới. Chỉ đánh giá sản phẩm khi đơn đã giao.</Typography>
    <Typography variant="h6" id="privacy">Dữ liệu demo</Typography>
    <Typography>Tài khoản, địa chỉ, đơn và nhật ký lưu trong PostgreSQL local; ảnh upload lưu trên máy. Email chỉ lưu hộp thư demo. Khởi động lại giữ dữ liệu. Quản trị viên có thể xem thông tin vận hành của demo.</Typography>
    <Typography variant="h6" id="terms">Phạm vi sử dụng</Typography>
    <Typography>Ứng dụng phục vụ học tập và trình diễn, chưa cung cấp giao dịch hoặc giao hàng thật. Trợ lý tìm sản phẩm trong danh mục local và chỉ tra đơn của tài khoản đăng nhập.</Typography>
    <Typography variant="h6" id="contact">Khi gặp lỗi</Typography>
    <Typography>Kiểm tra ứng dụng đang chạy, đăng nhập lại và xem hướng dẫn RUNNING.md trong dự án. Chưa có kênh hỗ trợ khách hàng bên ngoài.</Typography>
    <Button component={Link} to="/products">Xem sản phẩm</Button>
  </Stack></Paper></Box>
}
