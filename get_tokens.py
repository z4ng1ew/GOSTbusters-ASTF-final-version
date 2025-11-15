import requests
import sys
import json

def get_bank_token(base_url: str, client_id: str, client_secret: str) -> str | None:
    """
    Получает bank_token от указанного банка.

    Args:
        base_url: Базовый URL банка, например, 'https://vbank.open.bankingapi.ru'
        client_id: Ваш client_id команды (например, 'team179')
        client_secret: Ваш client_secret команды

    Returns:
        access_token (str) в случае успеха, иначе None.
    """
    token_url = f"{base_url.rstrip('/')}/auth/bank-token"
    params = {
        'client_id': client_id,
        'client_secret': client_secret
    }

    try:
        print(f"[INFO] Запрашиваю токен у: {token_url}")
        print(f"   Client ID: {client_id}")

        response = requests.post(token_url, params=params, timeout=30)

        if response.status_code == 200:
            token_data = response.json()
            access_token = token_data.get('access_token')
            if access_token:
                print("[SUCCESS] Токен успешно получен!")
                return access_token
            else:
                print("[ERROR] 'access_token' не найден в JSON-ответе.")
                print(f"   Полный ответ: {token_data}")
                return None
        elif response.status_code == 401:
             print(f"[ERROR] HTTP 401: Неверный client_id или client_secret.")
             print(f"   Проверьте, правильно ли указаны учетные данные для команды '{client_id}'.")
             print(f"   Тело ответа: {response.text}")
             return None
        else:
            print(f"[ERROR] Ошибка HTTP: {response.status_code}")
            print(f"   Тело ответа: {response.text}")
            return None

    except requests.exceptions.Timeout:
        print(f"[ERROR] Таймаут запроса к {token_url}")
        return None
    except requests.exceptions.ConnectionError:
        print(f"[ERROR] Не удалось подключиться к {token_url}. Проверьте URL и подключение к интернету.")
        return None
    except requests.exceptions.RequestException as e:
        print(f"[ERROR] Ошибка сети при запросе к {token_url}: {e}")
        return None
    except json.JSONDecodeError:
        print(f"[ERROR] Ответ от {token_url} не является корректным JSON. Тело ответа: {response.text}")
        return None

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("[USAGE] python get_tokens.py <bank_base_url> <client_id> <client_secret>")
        print("   Пример: python get_tokens.py https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO")
        sys.exit(1)

    bank_url = sys.argv[1]
    client_id = sys.argv[2]
    client_secret = sys.argv[3]

    token = get_bank_token(bank_url, client_id, client_secret)

    if token:
        print(f"[TOKEN] {token}")
        # Выводим только токен в последней строке для подстановки в другую команду
        print(token, end='')
    else:
        print("[FAILURE] Не удалось получить токен.")
        sys.exit(1)
