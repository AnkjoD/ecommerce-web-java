import { useContext, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import purchaseApi from '~/apis/purchase.api';
import { AppContext } from '~/contexts/app.context';
import { toast } from 'react-toastify';

export default function useAddToCart() {
  const { refreshCart } = useContext(AppContext);
  const [isSuccessPopupOpen, setIsSuccessPopupOpen] = useState(false);
  const addToCartMutation = useMutation({
    mutationFn: (body: { variant_id: string; quantity: number }) => purchaseApi.addToCart(body.variant_id, body.quantity),
    onSuccess: () => {
      refreshCart();
      toast.success('Đã thêm vào giỏ hàng');
    },
    onError: (error: any) => {
      console.error('Add to cart failed:', error);
      toast.error(error?.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại!');
    }
  });

  return {
    addToCart: addToCartMutation.mutate, // Hàm để gọi
    isPending: addToCartMutation.isPending, // Trạng thái loading
    isSuccessPopupOpen, // State popup
    handleClosePopup: () => setIsSuccessPopupOpen(false) // Hàm đóng popup
  };
}
