# AWS에서 스프링부트 CI / CD 파이프라인 구축하기
### CodePipeline, CodeBuild, CodeDeploy, Elastic Container Service 사용
### 깃허브 커밋 후 즉시 도커 이미지를 빌드하고, ECS에 Blue/Green 배포합니다.
### application.yml 파일은 S3에 저장돼 있어서 외부에 공개되지 않습니다.
<br><br/>
## 배포 아키텍처 & 진행 순서
<img width="899" alt="39 - 배포 아키텍처 그림" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/ce028e27-e6ae-4759-a21b-e0553b204a68">

- AWS CodePipeline은 소스, 빌드, 배포의 3 단계 스테이지로 구성돼 있습니다.
- 스프링부트 프로젝트에 필요한 config file인 application.yml 파일은 빌드 단계에서 S3로부터 다운로드 받기 때문에 외부 유출 우려가 적습니다. 
- ECS 내부에서 Fargate 옵션으로 배포하기 때문에, 컨테이너를 운영하기 위한 별도의 EC2를 셋팅할 필요가 없습니다.

<br><br/>

## CI/CD 파이프라인 제작 과정
### 0. 참고사항
- 설명용 이미지에서 보이는 access key 등의 보안 정보는 모두 폐기하였으며, 현재 유효하지 않습니다. 
- AWS 공식 문서를 참고하였습니다. [링크](https://docs.aws.amazon.com/ko_kr/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html)

### 1. 준비 >> AWS CLI 로그인, ECR 생성, S3에 yaml 파일 저장, ECS Role 생성

-  AWS 커맨드를 실행하기 위한 EC2 인스턴스를 OS 를 Amazon Linux를 선택하여 만들어주고, 해당 인스턴스 내부에서 aws configure 명령어를 실행하여 AWS 커맨드를 사용할 수 있게 해줍니다.
- AWS 에 로그인할 계정의 public 및 secret access key가 미리 만들어져 있어야 합니다.
  <img width="869" alt="01 - AWS CLI 실행용 인스턴스 만들고 AWS CLI 셋팅하기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/22454ef4-abcd-4e06-8cf9-36178f7f20b7">
- ECR(==Elastic Container Registry) 리포지토리 생성
  <img width="738" alt="02 - ECR 리포지토리 생성하기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/a500faf5-87e9-4c23-b984-8ffbc335a730">
- S3 버킷에 application.yaml 파일 저장
  <img width="555" alt="03 - 테스트 스프링부트 프로젝트용 야믈파일 내용물" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/4957ffba-db7c-4ba3-b1c9-69375b852009">
  <img width="720" alt="04 - S3 버킷에 스프링프로젝트용 야믈 업로드" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/e5bfa293-4dea-4edf-a0ec-1367f6bf38c3">
- [자습서](https://docs.aws.amazon.com/ko_kr/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html)의 안내에 따라서 AmazonECSTaskExecutionRolePolicy 생성
- 해당 Role은 본 스프링부트 프로젝트의 scripts 디렉토리 내의  [taskdef.json](https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/blob/master/scripts/taskdef.json)의 executionRoleArn 필드를 작성할 때 사용됩니다.
- taskdef.json은 ECS 내부에서 컨테이너를 어떻게 배포할 것인지를 정의한 파일입니다.
- 해당 파일은 CopePipeline의 배포 스테이지에서 사용됩니다.
  <img width="603" alt="05 - ECS Task Execution Role을 준비" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/229de8bf-d1d7-44b1-a941-a2a4e9f080a3">

<br>

### 2. Application Load Balancer 셋팅하기

- 기본 VPC 내부에서 서브넷 2 개의 아이디 확인
- 이 값들은 추후 ECS 내 Service를 만들 때 필요한 json 파일을 정의할 때 사용됩니다.
  <img width="826" alt="06 - 기본 VPC 내부에서 서브넷 아이디 두 개 확인" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/1c03c31e-292a-4298-84eb-06ef81ffcaa0">
- [자습서](https://docs.aws.amazon.com/ko_kr/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html) 안내에 따라서 ALB 만들고, 대상그룹 2개 설정해주기
  <img width="612" alt="07 - AWS의 자습서에서 안내하는대로 ALB 만들고 대상그룹 2개 셋팅하기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/97d6e3d5-2831-447c-aebf-6a340c585862">
- ALB 설정 세부사항 이미지
  <img width="741" alt="08 - ALB 설정 1" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/1b412c07-584f-4cf7-b82d-c7defde5ad89">
  <img width="739" alt="09 - ALB 설정2" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/6f83c7d5-1e83-42f6-a87f-2a037b4d47b0">
- ALB 80 포트 리스너에 target-group-1를, 8080 포트에 target-group-2를 생성하여 지정
- 바로 위의 이미지에서 [대상 그룹 생성]()이라는 링크를 클릭하면 됩니다.
  <img width="734" alt="10 - 대상그룹 설정 1" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/3cdcd44e-7a05-4aa0-8e14-85ff5186679b">
  <img width="748" alt="11 - ALB 설정2" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/9ffbb9ca-c8f7-405b-b033-5f81af1ac4bb">
- ALB가 사용하는 보안그룹에 80, 8080 포트 인바운드 규칙 추가 & Security Group ID 기록
- ALB가 사용하는 보안그룹의 아이디는 추후 ECS 내 Service를 만들 때 필요한 json 파일을 정의할 때 필요합니다.
  <img width="682" alt="38 - ALB 보안그룹에 80 과 8080 포트 인바운드 규칙 추가 필수" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/60c968e7-f34d-4595-a4ec-869069b39753">

### 3. ECS 클러스터 & 서비스 생성

- Fargate 옵션을 선택한 후 ECS 클러스터 생성
  <img width="549" alt="11 - ECS 클러스터 생성" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/d75752c1-42cf-4001-9446-18a102140957">
- AWS 커맨드로 ECS Service 생성
- ALB를 만들면서 기록해놓은 정보로 create-service.json 파일을 만들고, AWS CLI로 ECS Service를 만듭니다.
- json파일의 targetGroupArn 필드는 앞단계에서 만든 target-group-1 대상그룹의 ARN을 입력하면 됩니다.
- subnets 필드는 앞서 기록해 놓은 서브넷 아이디 2개를 입력하고, securityGroups 필드는 ALB가 사용하는 보안그룹 아이디를 입력합니다.
  <img width="707" alt="12 - ECS 클러스터 내 서비스 만들 때 필요한 대상그룹 ARN 확인하는 곳" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/ef2da285-4caf-4141-9873-ea52deb09ade">
  <img width="858" alt="13 - ECS 서비스 만들기 위한 json 정의" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/faa63fed-f9e9-41c6-a24d-23a4cde23ec9">
  <img width="930" alt="14 - AWS CLI 이용해서 ECS 내 서비스 생성" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/37587cfc-244b-4070-8c00-3c11f952c9ab">

<br>





