import { useParams, useSearchParams, Link } from 'react-router'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Box, Paper, Typography, Button, Stack, Alert, CircularProgress } from '@mui/material'
import http from '~/utils/http'
import type { SuccessResponseApi } from '~/types/utils.type'
import type { Order } from '~/types/purchase.type'
import path from '~/constants/path'
export default function DemoPayment() {
  const { id } = useParams(); const [params] = useSearchParams(); const nonce=params.get('nonce') || ''
  const info=useQuery({queryKey:['demo-payment',id,nonce],queryFn:()=>http.get<SuccessResponseApi<{order:Order;provider:string;mode:string}>>(`/payments/demo/${id}`,{params:{nonce}}),retry:false})
  const result=useMutation({mutationFn:(outcome:string)=>http.post<SuccessResponseApi<{isSuccess:boolean;order_code:string}>>(`/payments/demo/${id}/complete`,{nonce,outcome})})
  const methods=useQuery({queryKey:['saved-methods'],queryFn:()=>http.get<SuccessResponseApi<{id:string;provider:string}[]>>('/payments/methods'),enabled:result.isSuccess})
  const remove=useMutation({mutationFn:(methodId:string)=>http.delete(`/payments/methods/${methodId}`),onSuccess:()=>methods.refetch()})
  const message=(result.error as any)?.response?.data?.message || (info.error as any)?.response?.data?.message
  return <Box sx={{maxWidth:640,mx:'auto',py:6,px:2}}><Paper sx={{p:4,borderRadius:4}}><Stack spacing={3}>
    <Typography variant="h4">Thanh toán demo local</Typography>
    <Alert severity="info">Đây là mô phỏng VNPay/MoMo. Không nhập thẻ, không chuyển tiền và không kết nối cổng thanh toán thật.</Alert>
    {info.isLoading && <CircularProgress/>}{message && <Alert severity="error">{message}</Alert>}
    {info.data && <><Typography>Đơn {info.data.data.data.order.order_code} · {info.data.data.data.provider.toUpperCase()}</Typography><Typography variant="h5">{info.data.data.data.order.total.toLocaleString('vi-VN')} đ</Typography></>}
    {info.isSuccess && !result.isSuccess && <Stack direction="row" spacing={2}><Button variant="contained" disabled={result.isPending} onClick={()=>result.mutate('success')}>Mô phỏng thành công</Button><Button color="error" variant="outlined" disabled={result.isPending} onClick={()=>result.mutate('failure')}>Mô phỏng thất bại</Button></Stack>}
    {result.isSuccess && <Alert severity={result.data.data.data.isSuccess?'success':'warning'}>{result.data.data.data.isSuccess?'Thanh toán mô phỏng thành công.':'Thanh toán mô phỏng thất bại. Bạn có thể thử lại từ Đơn mua hoặc hủy đơn.'}</Alert>}
    {methods.data?.data.data.map(m=><Stack direction="row" justifyContent="space-between" key={m.id}><Typography>Phương thức {m.provider.toUpperCase()} demo đã lưu</Typography><Button onClick={()=>remove.mutate(m.id)} disabled={remove.isPending}>Xóa phương thức</Button></Stack>)}
    <Button component={Link} to={path.purchase}>Xem đơn mua</Button><Button component={Link} to={path.products}>Tiếp tục mua sắm</Button>
  </Stack></Paper></Box>
}
