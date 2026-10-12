param([string]$BaseUrl='http://localhost:3000/api')
$ErrorActionPreference='Stop'
$customerSession=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminSession=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminHeaders=@{'X-Client-App'='admin'}
function Request-Demo($method,$path,$session,$body=$null,$headers=@{}) {
    $options=@{Uri="$BaseUrl$path";Method=$method;WebSession=$session;Headers=$headers;TimeoutSec=15}
    if ($null -ne $body) {$options.ContentType='application/json';$options.Body=($body|ConvertTo-Json -Depth 15)}
    Invoke-RestMethod @options
}
function Assert-Demo($condition,$message) {if (-not $condition) {throw $message}}
$email='smoke-'+[guid]::NewGuid().ToString('N')+'@example.test'
$null=Request-Demo POST '/auth/register' $customerSession @{email=$email;password='SmokePass123!';full_name='Smoke Test'}
$null=Request-Demo POST '/auth/login' $customerSession @{email=$email;password='SmokePass123!'}
$me=Request-Demo GET '/auth/me' $customerSession
Assert-Demo ($me.data.email -eq $email) 'Cookie login failed'
$null=Request-Demo POST '/auth/refresh' $customerSession
$null=Request-Demo POST '/auth/admin/login' $adminSession @{email='admin@homura.test';password='DemoAdmin123!'} $adminHeaders
$sku='SMOKE-'+[guid]::NewGuid().ToString('N')
$product=Request-Demo POST '/admin/products' $adminSession @{
    name='Smoke Test Product';status='active';category_id='';description=@{short='Test fixture'};media=@{images=@()};
    variants=@(@{sku=$sku;price=100000;stock_quantity=1;options=@{size='M'}})
} $adminHeaders
Assert-Demo ($product.data.variants.Count -eq 1) 'Nested variant creation failed'
$variantId=$product.data.variants[0].id
$address=Request-Demo POST '/addresses' $customerSession @{recipient_name='Smoke Test';phone='0900000000';province='Hà Nội';district='Cầu Giấy';ward='Dịch Vọng';street='1 Demo Street'}
$addresses=Request-Demo GET '/addresses' $customerSession
Assert-Demo (-not (($addresses|ConvertTo-Json -Depth 20) -match 'password|tokenHash|refreshTokens')) 'Sensitive entity fields exposed'
$null=Request-Demo POST '/carts/items' $customerSession @{variant_id=$variantId;quantity=1}
$blocked=$false
try {$null=Request-Demo POST '/carts/items' $customerSession @{variant_id=$variantId;quantity=1}} catch {$blocked=([int]$_.Exception.Response.StatusCode -eq 400)}
Assert-Demo $blocked 'Cart stock overflow was not rejected'
$order=Request-Demo POST '/orders' $customerSession @{address_id=$address.data.id;payment_method='cod';coupon_code='DEMO10'}
Assert-Demo ($order.data.total -eq 120000) 'Order total does not include coupon and shipping correctly'
$history=Request-Demo GET '/orders' $customerSession
Assert-Demo ($history.data.total -eq 1) 'Order history failed'
$cart=Request-Demo GET '/carts' $customerSession
Assert-Demo ($cart.data.total_items -eq 0) 'Checkout did not clear the cart'
$adminOrders=Request-Demo GET '/admin/orders' $adminSession $null $adminHeaders
Assert-Demo (@($adminOrders.data.data|Where-Object {$_.id -eq $order.data.id}).Count -eq 1) 'Admin cannot see created order'
$null=Request-Demo POST "/orders/$($order.data.id)/cancel" $customerSession
$detail=Request-Demo GET "/products/$variantId" $customerSession
Assert-Demo ($detail.data.stock_quantity -eq 1) 'Cancellation did not restore stock'
$null=Request-Demo POST '/auth/logout' $customerSession
Write-Host 'PASS: cookie auth, refresh, admin product creation, safe addresses, stock validation, coupon checkout, history, admin visibility, cancellation.'
Write-Host 'Test fixtures remain in the local demo database. No external payment, email or shipping service was called.'
