import requests
import csv
import concurrent.futures
import threading
import json
import time

# --- 설정 ---
API_URL = "http://localhost:8083/join"  # 포트 8083 확인
TOTAL_USERS = 10000
THREADS = 50
OUTPUT_CSV_FILE = "users.csv"
FAILED_LOG_FILE = "failed_users.log"
PASSWORD_FOR_ALL = "password123"

# --- 헬퍼 함수 ---
def generate_email(index):
    return f"user{index}@example.com"

def generate_password():
    return PASSWORD_FOR_ALL

def register_user(index):
    """단일 사용자 등록 요청 및 결과 반환"""
    username = f"user{index}" # username은 요청 시 필요할 수 있으므로 유지
    email = generate_email(index)
    password = generate_password()

    data = {
        "username": username,
        "password": password,
        "confirmPassword": password,
        "email": email
    }

    try:
        response = requests.post(API_URL, json=data, timeout=10)

        # 성공 코드는 실제 API 확인 결과 201 이었음
        if response.status_code == 201: # 성공 (Created)
             # --- 수정: username 대신 email 반환 ---
             return ('success', (email, password))
        else:
            # 실패 (HTTP 상태 코드가 201 아님)
            try:
                 error_msg = response.json().get('errorMessage', response.text[:150])
            except json.JSONDecodeError:
                 error_msg = response.text[:150]
            # username은 로그 기록용으로 사용
            return ('fail', (username, response.status_code, error_msg))

    except requests.exceptions.RequestException as e:
        # 네트워크 오류 등 요청 자체 실패 (username 로깅)
        return ('error', (username, str(e)))

# --- 메인 실행 ---
if __name__ == "__main__":
    start_time = time.time()
    print(f"--- Starting user creation for {TOTAL_USERS} users using {THREADS} threads ---")
    print(f"--- Target API: {API_URL} ---")

    try:
        with open(OUTPUT_CSV_FILE, 'w', newline='', encoding='utf-8') as csvfile, \
             open(FAILED_LOG_FILE, 'w', encoding='utf-8') as fail_log:

            writer = csv.writer(csvfile)
            # --- 수정: CSV 헤더 변경 ---
            writer.writerow(['email', 'password'])

            success_count = 0
            fail_count = 0
            error_count = 0

            with concurrent.futures.ThreadPoolExecutor(max_workers=THREADS) as executor:
                futures = [executor.submit(register_user, i) for i in range(1, TOTAL_USERS + 1)]

                for i, future in enumerate(concurrent.futures.as_completed(futures)):
                    try:
                        status, data = future.result()

                        if status == 'success':
                            # --- 수정: email, password 쓰기 ---
                            writer.writerow(data)
                            success_count += 1
                            if success_count % 100 == 0:
                                 # --- 수정: email 로깅 ---
                                 print(f"[Success: {success_count}/{TOTAL_USERS}] User with email '{data[0]}' created")
                        elif status == 'fail':
                            username, code, msg = data # 실패/오류 로그에는 username 사용
                            fail_log.write(f"FAIL - User: {username} | Status: {code} | Msg: {msg}\\n")
                            fail_count += 1
                            if fail_count % 100 == 0:
                                print(f"[Fail: {fail_count}] User '{username}' failed (Status: {code}, Msg: {msg[:50]}...)")
                        elif status == 'error':
                            username, err_msg = data # 실패/오류 로그에는 username 사용
                            fail_log.write(f"ERROR - User: {username} | Error: {err_msg}\\n")
                            error_count += 1
                            if error_count % 100 == 0:
                               print(f"[Error: {error_count}] User '{username}' request error: {err_msg[:50]}...")

                    except Exception as exc:
                        print(f"An exception occurred retrieving result: {exc}")
                        error_count += 1
                        fail_log.write(f"ERROR - Retrieving Future Result | Error: {exc}\\n")


                    processed_count = success_count + fail_count + error_count
                    if processed_count % 1000 == 0 or processed_count == TOTAL_USERS :
                        elapsed = time.time() - start_time
                        print(f"--- Progress: {processed_count}/{TOTAL_USERS} users processed in {elapsed:.2f} seconds ---")

    except IOError as e:
        print(f"Error opening or writing file: {e}")
    except Exception as e:
        print(f"An unexpected error occurred during setup or execution: {e}")


    end_time = time.time()
    print("\\n--- User Creation Summary ---")
    print(f"Total users attempted: {TOTAL_USERS}")
    print(f"Successful creations (Status 201): {success_count}")
    print(f"Failed creations (API error, e.g., 400): {fail_count}")
    print(f"Request errors (Network, Timeout, etc.): {error_count}")
    print(f"Successfully created users saved to: {OUTPUT_CSV_FILE}")
    print(f"Failed/Error logs saved to: {FAILED_LOG_FILE}")
    print(f"Total execution time: {end_time - start_time:.2f} seconds")