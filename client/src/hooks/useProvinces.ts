import { useQuery } from '@tanstack/react-query'


export interface Ward {
  name: string
  code: number
}

export interface District {
  name: string
  code: number
  wards: Ward[]
}

export interface Province {
  name: string
  code: number
  districts: District[]
}

export default function useProvinces() {
  return useQuery({
    queryKey: ['provinces'],
    queryFn: async () => {
      // Small static demo dataset; not an authoritative administrative directory.
      return [
        { name: 'Hà Nội', code: 1, districts: [{ name: 'Cầu Giấy', code: 101, wards: [{ name: 'Dịch Vọng', code: 10101 }] }] },
        { name: 'Hồ Chí Minh', code: 79, districts: [{ name: 'Quận 1', code: 7901, wards: [{ name: 'Bến Nghé', code: 790101 }] }] },
        { name: 'Đà Nẵng', code: 48, districts: [{ name: 'Hải Châu', code: 4801, wards: [{ name: 'Hải Châu I', code: 480101 }] }] }
      ] as Province[]
    },
    staleTime: Infinity, // Dữ liệu tỉnh/thành phố hiếm khi thay đổi nên cache vĩnh viễn trong session
    retry: 3
  })
}
