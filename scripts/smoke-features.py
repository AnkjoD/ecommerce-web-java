"""Exercise local-demo APIs, including a real race for the last stock unit.
Requires the Windows demo to be running. Uses only Python's standard library.
Creates fixtures in the demo database; products are archived after success.
"""
import concurrent.futures
import http.cookiejar
import json
import threading
import urllib.error
import urllib.request
import uuid

BASE = 'http://127.0.0.1:6000/api'


class Session:
    def __init__(self, admin=False):
        self.admin = admin
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, body=None, expected=200):
        headers = {'Content-Type': 'application/json'}
        if self.admin:
            headers['X-Client-App'] = 'admin'
        request = urllib.request.Request(BASE + path, None if body is None else json.dumps(body).encode(), headers, method=method)
        try:
            with self.opener.open(request, timeout=30) as response:
                code, raw = response.status, response.read()
        except urllib.error.HTTPError as ex:
            code, raw = ex.code, ex.read()
        assert code == expected, (method, path, code, raw.decode())
        result = json.loads(raw) if raw else {}
        return result.get('data', result)


admin = Session(True)
admin.call('POST', '/auth/admin/login', {'email': 'admin@homura.test', 'password': 'DemoAdmin123!'})
config = admin.call('GET', '/payments/config')
assert config['mode'] == 'demo', 'This script only supports the local demo profile'
products = []
parent = admin.call('POST', '/admin/categories', {'name': 'Demo parent'})
child = admin.call('POST', '/admin/categories', {'name': 'Demo child', 'parent_id': parent['id']})
admin.call('PATCH', '/admin/categories/' + parent['id'], {'parent_id': child['id']}, 400)
admin.call('DELETE', '/admin/categories/' + parent['id'], expected=400)
admin.call('DELETE', '/admin/categories/' + child['id'])
admin.call('DELETE', '/admin/categories/' + parent['id'])
print('PASS: category hierarchy rejects cycles and unsafe deletion')


def customer():
    s = Session()
    s.call('POST', '/auth/register', {'email': str(uuid.uuid4()) + '@example.test', 'password': 'DemoTest123!', 'full_name': 'Local API Test'}, 201)
    address = s.call('POST', '/addresses', {'recipient_name': 'Test', 'phone': '0900000000', 'province': 'Hà Nội', 'district': 'Cầu Giấy', 'ward': 'Dịch Vọng', 'street': '1 Demo Street'}, 201)
    return s, address['id']


def product(stock=3):
    p = admin.call('POST', '/admin/products', {'name': 'Local feature fixture', 'status': 'active', 'variants': [{'sku': str(uuid.uuid4()), 'price': 100000, 'stock_quantity': stock, 'options': {'size': 'M'}}]})
    products.append(p['id'])
    original = p['variants'][0]
    updated = admin.call('PATCH', '/admin/products/' + p['id'] + '/metadata', {
        'tags': ['local-test'], 'variants': [{
            'id': original['id'], 'sku': original['sku'], 'price': 100000,
            'stock_quantity': stock, 'weight': 750, 'options': {'size': 'M', 'color': 'Black'}
        }]
    })
    assert updated['variants'][0]['options']['color'] == 'Black'
    assert updated['tags'] == ['local-test']
    return p['variants'][0]['id']


first, address = customer()
second, second_address = customer()
v = product(1)
for s in (first, second):
    s.call('POST', '/carts/items', {'variant_id': v, 'quantity': 1})
barrier = threading.Barrier(2)


def race(args):
    s, a = args
    barrier.wait(timeout=10)
    try:
        return s.call('POST', '/orders', {'address_id': a, 'payment_method': 'cod'})
    except AssertionError as ex:
        assert ex.args[0][2] == 400, ex
        return None


with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(race, [(first, address), (second, second_address)]))
assert sum(x is not None for x in results) == 1
assert first.call('GET', '/products/' + v)['stock_quantity'] == 0
winner = first if results[0] else second
order = results[0] or results[1]
winner.call('POST', '/orders/' + order['id'] + '/cancel')
assert first.call('GET', '/products/' + v)['stock_quantity'] == 1
print('PASS: simultaneous checkout for one stock unit; exactly one order, cancellation restores stock')

# Use new users so losing carts above cannot contaminate later checkout.
owner, a = customer()
stranger, _ = customer()
coupon = admin.call('POST', '/coupons', {'code': 'LOCAL-' + uuid.uuid4().hex[:8], 'discount_type': 'percent', 'discount_value': 10, 'usage_limit': 5})
admin.call('PATCH', '/coupons/' + coupon['id'], {'discount_value': 15})
for provider in ('vnpay', 'momo'):
    v = product()
    owner.call('POST', '/carts/items', {'variant_id': v, 'quantity': 1})
    order = owner.call('POST', '/orders', {'address_id': a, 'payment_method': provider, 'coupon_code': coupon['code']})
    assert order['total'] == 115000
    if provider == 'vnpay':
        url = owner.call('POST', '/payments/create-url?saveCard=true', {'order_id': order['id']})['url']
    else:
        url = owner.call('POST', '/payments/momo/create/' + order['id'] + '?saveCard=true', {})
    nonce = url.split('nonce=')[1]
    path = '/payments/demo/' + order['id']
    stranger.call('GET', path + '?nonce=' + nonce, expected=404)
    owner.call('POST', path + '/complete', {'nonce': 'forged', 'outcome': 'success'}, 403)
    owner.call('POST', path + '/complete', {'nonce': nonce, 'outcome': 'failure'})
    if provider == 'vnpay':
        url = owner.call('POST', '/payments/create-url?saveCard=true', {'order_id': order['id']})['url']
    else:
        url = owner.call('POST', '/payments/momo/create/' + order['id'] + '?saveCard=true', {})
    new_nonce = url.split('nonce=')[1]
    owner.call('POST', path + '/complete', {'nonce': nonce, 'outcome': 'success'}, 403)
    result = owner.call('POST', path + '/complete', {'nonce': new_nonce, 'outcome': 'success'})
    assert result['isSuccess']
    owner.call('POST', path + '/complete', {'nonce': new_nonce, 'outcome': 'success'})
    for state in ('processing', 'shipping', 'delivered'):
        admin.call('PATCH', '/admin/orders/' + order['id'] + '/status', {'status': state})
    p = order['items'][0]['variant']['product']['id']
    review = owner.call('POST', '/reviews', {'product_id': p, 'rating': 5, 'comment': 'Local demo verified'})
    admin.call('PATCH', '/admin/reviews/' + review['id'], {'admin_reply': 'Thank you'})
    assert owner.call('GET', '/reviews/product/' + p)['stats']['avg'] == 5
    for _ in range(2):
        admin.call('PATCH', '/admin/orders/' + order['id'] + '/payment-status', {'status': 'refunded'})
    assert owner.call('GET', '/products/' + v)['stock_quantity'] == 3
    print('PASS:', provider, 'failure/retry/success/replay, delivery, review/reply, idempotent refund')

methods = owner.call('GET', '/payments/methods')
assert len(methods) == 2
for method in methods:
    stranger.call('DELETE', '/payments/methods/' + method['id'], expected=404)
    owner.call('DELETE', '/payments/methods/' + method['id'])
admin.call('DELETE', '/coupons/' + coupon['id'])
assert admin.call('GET', '/admin/mail/outbox')['total'] > 0
assert admin.call('GET', '/admin/audit-logs')['total'] > 0
assert len(admin.call('GET', '/admin/stats/overview')['history']) == 30
assert admin.call('GET', '/admin/orders?search=' + order['order_code'])['total'] == 1
reply = stranger.call('POST', '/chat/completions', {'message': 'đơn hàng'})
assert order['order_code'] not in reply['response']
for p in products:
    admin.call('DELETE', '/admin/products/' + p)
print('PASS: coupons, saved methods ownership, email outbox, audit, stats, search, private chat')
print('All checks used local demo APIs; fixtures remain in the demo database with products archived.')
