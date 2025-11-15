# get_sbank_consent.py
import requests
import sys
import time
import json

# --- Конфигурация ---
# URL теперь получаем как аргумент
CLIENT_ID_TEAM = "team179"
CLIENT_SECRET = "JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO"
# Для согласия обычно нужен ID клиента, а не команды. Часто это формат teamXXX-1
CLIENT_ID_USER = f"{CLIENT_ID_TEAM}-1" # Предполагаемый ID клиента

def get_bank_token(bank_url):
    """Получает bank_token для SBank."""
    token_url = f"{bank_url}/auth/bank-token"
    params = {
        'client_id': CLIENT_ID_TEAM,
        'client_secret': CLIENT_SECRET
    }

    try:
        print(f"🔄 Запрашиваю токен у: {token_url}")
        response = requests.post(token_url, params=params, timeout=30)

        if response.status_code == 200:
            token_data = response.json()
            access_token = token_data.get('access_token')
            if access_token:
                print("✅ Токен SBank успешно получен!")
                return access_token
            else:
                print("❌ Ошибка: 'access_token' не найден в ответе.")
                print(f"   Полный ответ: {token_data}")
                return None
        else:
            print(f"❌ Ошибка HTTP при получении токена: {response.status_code}")
            print(f"   Тело ответа: {response.text}")
            return None

    except requests.exceptions.RequestException as e:
        print(f"❌ Ошибка сети при запросе токена: {e}")
        return None

def request_consent(bank_token, bank_url):
    """Запрашивает согласие на доступ к данным клиента."""
    consent_url = f"{bank_url}/account-consents/request"
    headers = {
        "Authorization": f"Bearer {bank_token}",
        "X-Requesting-Bank": CLIENT_ID_TEAM, # ID команды, не клиента
        "Content-Type": "application/json"
    }
    # Тело запроса согласия
    consent_body = {
        "client_id": CLIENT_ID_USER, # ID клиента
        "permissions": [
            "ReadAccountsDetail",
            "ReadBalances",
            "ReadTransactionsDetail",
            # Добавьте другие разрешения, если нужно
            # "ReadProductAgreements", # Если планируется тестировать /product-agreements
            # "ReadStandingOrdersDetail",
            # "ReadDomesticPaymentsDetail"
        ],
        "reason": "Запрос согласия для хакатона VTB API 2025", # Причина запроса
        "requesting_bank": CLIENT_ID_TEAM, # ID команды
        "requesting_bank_name": "GOSTbusters Team 179" # Имя команды
    }

    try:
        print(f"\n🔄 Отправляю запрос на согласие: {consent_url}")
        response = requests.post(consent_url, headers=headers, json=consent_body, timeout=30)

        if response.status_code == 200:
            consent_response = response.json()
            # SBank возвращает request_id, а не consent_id сразу
            request_id = consent_response.get('request_id')
            status = consent_response.get('status')
            auto_approved = consent_response.get('auto_approved', False)

            if request_id:
                print(f"✅ Запрос на согласие отправлен! (ID: {request_id})")
                print(f"   Статус: {status}")
                print(f"   Авто-одобрение: {auto_approved}")
                if not auto_approved:
                     print("\n--- 🔁 РУЧНОЕ ОДОБРЕНИЕ НЕОБХОДИМО ---")
                     print(f"   1. Перейдите в UI SBank: {bank_url}/client/consents.html")
                     print(f"   2. Войдите как клиент '{CLIENT_ID_USER}' (если требуется).")
                     print(f"   3. Найдите запрос согласия и нажмите 'Подписать'.")
                     print(f"   4. После подтверждения статус изменится на 'Authorized'.")
                     print("----------------------------------------\n")
                return request_id
            else:
                print("❌ Ошибка: 'request_id' не найден в ответе на запрос согласия.")
                print(f"   Полный ответ: {consent_response}")
                return None
        else:
            print(f"❌ Ошибка HTTP при запросе согласия: {response.status_code}")
            print(f"   Тело ответа: {response.text}")
            return None

    except requests.exceptions.RequestException as e:
        print(f"❌ Ошибка сети при запросе согласия: {e}")
        return None

def check_consent_status(bank_token, request_id, bank_url):
    """Проверяет статус согласия по request_id."""
    # Важно: SBank использует request_id в GET-запросе, как указано в документации.
    consent_status_url = f"{bank_url}/account-consents/{request_id}"
    headers = {
        "Authorization": f"Bearer {bank_token}",
        "X-Fapi-Interaction-Id": CLIENT_ID_TEAM # Опциональный заголовок, можно использовать ID команды
    }

    print(f"\n🔄 Проверяю статус согласия (ID: {request_id})...")

    try:
        response = requests.get(consent_status_url, headers=headers, timeout=30)
        if response.status_code == 200:
            consent_details = response.json()
            consent_data = consent_details.get('data', {})
            consent_id = consent_data.get('consentId')
            status = consent_data.get('status')

            print(f"   Статус согласия: {status}")
            if consent_id:
                print(f"   ID согласия (consent_id): {consent_id}")

            if status.lower() == 'authorized': # Статус "Authorized" означает активное согласие
                print("✅ Согласие активно!")
                return consent_id
            elif status.lower() in ['rejected', 'revoked', 'expired']:
                 print(f"❌ Согласие в статусе '{status}'. Запросите новое.")
                 return None
            else:
                # Статус pending или awaiting_authorisation
                print(f"   Согласие все еще ожидает подтверждения (status: {status}).")
                return None # Возвращаем None, если не активно

        else:
            print(f"❌ Ошибка HTTP при проверке статуса: {response.status_code}")
            print(f"   Тело ответа: {response.text}")
            return None

    except requests.exceptions.RequestException as e:
        print(f"❌ Ошибка сети при проверке статуса: {e}")
        return None

def main():
    if len(sys.argv) != 2:
        print("❌ Использование: python get_sbank_consent.py <bank_base_url>")
        print("   Пример: python get_sbank_consent.py https://sbank.open.bankingapi.ru")
        sys.exit(1)

    bank_url = sys.argv[1]

    print(f"--- 🏦 Получение согласия для {bank_url} ---")

    # 1. Получить токен SBank
    bank_token = get_bank_token(bank_url)
    if not bank_token:
        print("❌ Не удалось получить токен. Завершение.")
        sys.exit(1)

    # 2. Запросить согласие
    consent_request_id = request_consent(bank_token, bank_url)
    if not consent_request_id:
        print("❌ Не удалось отправить запрос на согласие. Завершение.")
        sys.exit(1)

    # 3. Цикл ожидания активации согласия
    max_attempts = 10 # Максимальное количество попыток проверки
    delay = 10 # Задержка между попытками в секундах

    consent_id = None
    for attempt in range(1, max_attempts + 1):
        print(f"\n--- Попытка {attempt}/{max_attempts} ---")
        consent_id = check_consent_status(bank_token, consent_request_id, bank_url)
        if consent_id:
            break # Согласие активно, выходим из цикла
        if attempt < max_attempts:
            print(f"   Жду {delay} секунд перед следующей проверкой...")
            time.sleep(delay)
        else:
            print(f"\n⏰ Время ожидания истекло. Согласие не было активировано за {max_attempts * delay} секунд.")
            print("   Убедитесь, что вы подтвердили согласие в UI SBank.")

    if consent_id:
        print("\n--- ✅ ВСЁ ГОТОВО ---")
        print(f"Получен consent_id для {bank_url}: {consent_id}")
        print(f"Токен банка: {bank_token}")
        print(f"X-Requesting-Bank: {CLIENT_ID_TEAM}")
        print(f"Client ID (для запросов): {CLIENT_ID_USER}")
        print("\n--- Завершено ---")
        # Выводим только consent_id для подстановки в другую команду
        print(consent_id, end='')
    else:
        print("\n❌ Не удалось получить активное согласие. Завершение.")
        sys.exit(1)

if __name__ == "__main__":
    main()
