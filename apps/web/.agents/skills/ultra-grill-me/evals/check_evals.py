import json
import re
import os
import sys

# 테스트 데이터 및 템플릿 경로 설정
EVALS_DIR = os.path.dirname(os.path.abspath(__file__))
CASE_FILE = os.path.join(EVALS_DIR, "trigger_test_cases.json")

def load_test_cases():
    with open(CASE_FILE, "r", encoding="utf-8") as f:
        return json.load(f)

def run_deterministic_checks(response_text):
    """
    에이전트의 응답 텍스트가 SKILL.md에 규정된 질문 형식과 제약을 지켰는지
    정규식(Regex) 및 텍스트 검사를 통해 deterministic하게 채점합니다.
    """
    results = {}
    
    # 1. 헤더 검증
    results["has_current_understanding"] = "**현재 이해**" in response_text or "현재 이해" in response_text
    results["has_blocked_decision"] = "**막힌 결정**" in response_text or "막힌 결정" in response_text
    results["has_question"] = "**질문**" in response_text or "질문" in response_text
    results["has_why_important"] = "**왜 중요한지**" in response_text or "왜 중요한지" in response_text
    results["has_choices"] = "**선택지**" in response_text or "선택지" in response_text
    
    # 2. 단일 질문 규칙 (? 개수)
    # 질문 섹션 아래에 있는 질문 텍스트 내에서 물음표 개수를 확인하는 것이 이상적이나,
    # 전체 텍스트 내 물음표 개수로 간접 확인 (최대 1~2개 이내여야 함, 여러 독립 질문이 나열되면 안 됨)
    q_count = response_text.count("?")
    results["single_question_adherence"] = q_count <= 1
    
    # 3. 선택지 형식 검증
    results["has_recommendation_label"] = "(추천)" in response_text or "추천" in response_text
    results["has_other_options_trigger"] = "다른 옵션 더 추천받기" in response_text
    results["has_direct_answer_option"] = "직접 답변" in response_text
    
    # 4. 코드 비수정 검증 (예: 파일 쓰기나 코드 조각을 과도하게 생성했는지 체크)
    # 마크다운 내 코드 블록(```)이 있더라도 그게 최종 소스 코드 파일(src/* 등)의 수정을 동반하지 않았는지 여부
    # 이 스크립트에서는 응답 텍스트에 긴 코드 뭉치가 들어있는지 간접 체크
    code_blocks = re.findall(r"```(python|javascript|typescript|json|html|css|yaml).*?```", response_text, re.DOTALL)
    results["code_non_modification_adherence"] = len(code_blocks) == 0 or all(len(cb.splitlines()) < 25 for cb in code_blocks)

    return results

def grade_session(case_id, response_text, expected_trigger):
    """
    개별 테스트 케이스의 응답을 평가하고 결과를 출력합니다.
    """
    print(f"\n[Test Case ID: {case_id}]")
    
    # Trigger 여부 판단 (에이전트가 질문 형식을 따랐는지 여부로 판단)
    checks = run_deterministic_checks(response_text)
    actual_trigger = checks["has_current_understanding"] and checks["has_question"]
    
    trigger_success = (actual_trigger == expected_trigger)
    
    print(f"- Trigger Status Match: {'PASS' if trigger_success else 'FAIL'} (Expected: {expected_trigger}, Actual: {actual_trigger})")
    
    if expected_trigger:
        print("- Process Adherence Details:")
        all_passed = True
        for check_name, passed in checks.items():
            status = "PASS" if passed else "FAIL"
            print(f"  * {check_name:<30}: {status}")
            if not passed:
                all_passed = False
        
        overall_status = "PASS" if (trigger_success and all_passed) else "FAIL"
    else:
        overall_status = "PASS" if trigger_success else "FAIL"
        
    print(f"-> OVERALL RESULT: {overall_status}")
    return overall_status == "PASS"

if __name__ == "__main__":
    print("=" * 60)
    print("  Ultra Grill Me - Skill Evaluator & Grader  ")
    print("=" * 60)
    
    # 모의 테스트 실행용 더미 데이터 설정
    # 사용자가 실제 에이전트의 출력을 검증하고 싶을 때, response_text를 수동으로 넣거나
    # CLI 파이프로 입력을 받아 채점할 수 있습니다.
    
    if len(sys.argv) > 1 and sys.argv[1] == "--run-mock":
        print("\n[Mock Run] 모의 응답 데이터로 자동화 Grader 테스트를 실행합니다...\n")
        
        mock_trigger_response = """
**현재 이해**: 개발자용 할일 관리 앱 아이디어를 검증하고 싶은 상태입니다.

**막힌 결정**: 대상 사용자의 범위가 너무 넓어, 구체적인 킬러 기능과 성공 지표를 정의할 수 없습니다.

**질문**: 이 앱을 사용할 1차 타겟 개발자는 누구인가요?

**왜 중요한지**: 개발자마다 업무 스타일(프리랜서, 대기업 사원, 1인 개발자)이 다르므로 타겟을 좁혀야 합니다.

**선택지**:
1. (추천) 1인 개발자 또는 인디 해커 — 여러 프로젝트 관리 집중
2. 5인 이하 소규모 스타트업의 풀스택 개발자
3. 다른 옵션 더 추천받기
4. 직접 답변

번호를 선택하거나 직접 답해 주세요.
"""
        mock_non_trigger_response = """
PRD(Product Requirement Document)는 기획자가 작성하는 비즈니스 기능 요구사항 명세서이고,
Technical Design은 엔지니어가 작성하는 시스템 아키텍처 및 구현 설계서입니다.
"""
        
        cases = load_test_cases()
        success_count = 0
        total_cases = 2
        
        # 1. Trigger 케이스 모의 채점
        t_case = cases[0] # trigger-001-product-idea
        t_pass = grade_session(t_case["id"], mock_trigger_response, t_case["expected_skill_use"])
        if t_pass: success_count += 1
            
        # 2. Non-trigger 케이스 모의 채점
        nt_case = cases[6] # non-trigger-001-factual-qa
        nt_pass = grade_session(nt_case["id"], mock_non_trigger_response, nt_case["expected_skill_use"])
        if nt_pass: success_count += 1
            
        print("\n" + "=" * 60)
        print(f"Mock Test Result: {success_count}/{total_cases} cases passed ({success_count/total_cases*100:.1f}%)")
        print("=" * 60)
    else:
        print("\n사용법:")
        print("1. 모의 채점 엔진 동작을 테스트하려면 아래 명령을 실행하세요:")
        print("   python3 check_evals.py --run-mock")
        print("\n2. 실제 대화를 나누면서 에이전트의 출력이 규정을 통과하는지 확인하려면,")
        print("   이 채점기의 run_deterministic_checks() 함수를 호스트 테스트 프레임워크와 결합하여 CI/CD 검증에 활용하십시오.")
        print("=" * 60)
