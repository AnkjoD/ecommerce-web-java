import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import purchaseApi from '~/apis/purchase.api'
import paymentApi from '~/apis/payment.api'
import { purchaseStatus } from '~/constants/purchase'
import path from '~/constants/path'
import { useNavigate } from 'react-router'
import { toast } from 'react-toastify'

export default function useBuyPurchases() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [isSuccessPopupOpen, setIsSuccessPopupOpen] = useState(false)
  const [isCheckoutModalOpen, setIsCheckoutModalOpen] = useState(false)

  const buyPurchasesMutation = useMutation({
    mutationFn: (body: { address_id: string; payment_method: string; coupon_code?: string; save_card?: boolean; item_ids?: string[] }) =>
      purchaseApi.buyPurchases(body),
    onSuccess: async (response, variables) => {
      const order = response.data.data

      // Nếu là VNPay -> Lấy link và redirect theo phương thức POST (với Params)
      if (variables.payment_method === 'vnpay') {
        try {
          const res = await paymentApi.createVnPayUrl(order.id, variables.save_card)
          const { url } = res.data.data

          window.location.assign(url)
          return
        } catch (error) {
          toast.error('Đơn hàng đã tạo, nhưng chưa mở được VNPay. Bạn có thể xem đơn trong lịch sử.')
          queryClient.invalidateQueries({ queryKey: ['purchases'] })
          navigate(path.purchase)
          return
        }
      }

      // Nếu là MoMo -> Lấy link và redirect
      if (variables.payment_method === 'momo') {
        try {
          const res = await paymentApi.createMomoUrl(order.id, variables.save_card)
          window.location.href = res.data.data // Redirect to MoMo
          return
        } catch (error) {
          toast.error('Đơn đã tạo nhưng chưa mở được thanh toán; bạn có thể thử lại từ Đơn mua')
          queryClient.invalidateQueries({ queryKey: ['purchases'] })
          navigate(path.purchase)
          return
        }
      }

      // Nếu là COD hoặc VNPay lỗi quay lại -> Hiện popup thành công
      setIsSuccessPopupOpen(true)
      queryClient.invalidateQueries({ queryKey: ['purchases', { status: purchaseStatus.inCart }] })
    },
    onError: (error: any) => {
      console.error('Order creation failed:', error)
      toast.error(error?.response?.data?.message || 'Đặt hàng không thành công. Vui lòng thử lại.')
    }
  })

  return {
    buyPurchases: buyPurchasesMutation.mutate,
    isPending: buyPurchasesMutation.isPending,
    isSuccessPopupOpen,
    isCheckoutModalOpen,
    setIsCheckoutModalOpen,
    handleClosePopup: () => {
      setIsSuccessPopupOpen(false)
      navigate(path.purchase)
    }
  }
}
