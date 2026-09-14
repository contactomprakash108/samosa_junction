import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { StaffLayout } from '@/layouts/StaffLayout'
import { CartPage } from '@/pages/CartPage'
import { CheckoutPage } from '@/pages/CheckoutPage'
import { AssistantPage } from '@/pages/AssistantPage'
import { BrandPage } from '@/pages/BrandPage'
import { HelpFaqPage } from '@/pages/HelpFaqPage'
import { HelpHubPage } from '@/pages/HelpHubPage'
import { SupportPage } from '@/pages/SupportPage'
import { ComplaintDetailPage } from '@/pages/ComplaintDetailPage'
import { ComplaintsPage } from '@/pages/ComplaintsPage'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { NewComplaintPage } from '@/pages/NewComplaintPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { OrderDetailPage } from '@/pages/OrderDetailPage'
import { OrdersPage } from '@/pages/OrdersPage'
import { ProductDetailPage } from '@/pages/ProductDetailPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { WalletPage } from '@/pages/WalletPage'
import { StaffComplaintDetailPage } from '@/pages/staff/StaffComplaintDetailPage'
import { StaffComplaintsPage } from '@/pages/staff/StaffComplaintsPage'
import { StaffDashboardPage } from '@/pages/staff/StaffDashboardPage'
import { StaffInventoryPage } from '@/pages/staff/StaffInventoryPage'
import { StaffKitchenPage } from '@/pages/staff/StaffKitchenPage'
import { StaffOrderDetailPage } from '@/pages/staff/StaffOrderDetailPage'
import { StaffOrdersPage } from '@/pages/staff/StaffOrdersPage'
import { StaffProductFormPage } from '@/pages/staff/StaffProductFormPage'
import { StaffProductsPage } from '@/pages/staff/StaffProductsPage'
import { StaffSupportPage } from '@/pages/staff/StaffSupportPage'
import { GuestRoute } from '@/routes/GuestRoute'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { StaffRoute } from '@/routes/StaffRoute'


export function AppRoutes() {
  return (
    <Routes>
      <Route element={<StaffRoute />}>
        <Route element={<StaffLayout />}>
          <Route path="staff" element={<StaffDashboardPage />} />
          <Route path="staff/kitchen" element={<StaffKitchenPage />} />
          <Route path="staff/orders" element={<StaffOrdersPage />} />
          <Route path="staff/orders/:orderId" element={<StaffOrderDetailPage />} />
          <Route path="staff/inventory" element={<StaffInventoryPage />} />
          <Route path="staff/products" element={<StaffProductsPage />} />
          <Route path="staff/products/new" element={<StaffProductFormPage />} />
          <Route path="staff/products/:productId" element={<StaffProductFormPage />} />
          <Route path="staff/complaints" element={<StaffComplaintsPage />} />
          <Route path="staff/complaints/:complaintId" element={<StaffComplaintDetailPage />} />
          <Route path="staff/support" element={<StaffSupportPage />} />
        </Route>
      </Route>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="menu" element={<Navigate to="/" replace />} />
        <Route path="products/:productId" element={<ProductDetailPage />} />
        <Route path="assistant" element={<AssistantPage />} />
        <Route path="help" element={<HelpHubPage />} />
        <Route path="help/guides" element={<HelpFaqPage />} />
        <Route path="help/chat" element={<Navigate to="/support" replace />} />
        <Route path="soon/help" element={<Navigate to="/help/guides" replace />} />
        <Route path="soon/support" element={<Navigate to="/help/chat" replace />} />
        <Route path="soon/agent" element={<Navigate to="/assistant" replace />} />
        <Route path="about" element={<BrandPage />} />
        <Route path="story" element={<BrandPage />} />
        <Route path="careers" element={<BrandPage />} />
        <Route path="privacy" element={<BrandPage />} />
        <Route path="terms" element={<BrandPage />} />
        <Route path="contact" element={<BrandPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route element={<GuestRoute />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="cart" element={<CartPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="wallet" element={<WalletPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="complaints" element={<ComplaintsPage />} />
          <Route path="complaints/new" element={<NewComplaintPage />} />
          <Route path="complaints/:complaintId" element={<ComplaintDetailPage />} />
          <Route path="support" element={<SupportPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="kitchen" element={<Navigate to="/staff/kitchen" replace />} />
        </Route>
        <Route path="home" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
