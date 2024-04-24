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
- [자습서의 해당 부분](https://docs.aws.amazon.com/ko_kr/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html#tutorials-ecs-ecr-codedeploy-loadbal)의 안내에 따라서 ALB 만들고, 대상그룹 2개 설정해주기
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

### 4. CodeDeploy Application 생성

- 아래와 같이 CodeDeploy Application을 생성해줍니다.
  <img width="567" alt="15 - Code Deploy 애플리케이션 생성" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/3d5cb57f-bb1c-4c7c-8d90-5d9b29ed3c4d">
- 그 후 해당 Application 내에서 배포그룹을 만들어줍니다.
- 배포 그룹을 만드는 과정에서 필요한 CodeDeployECSRole을 선택해줍니다. 없을 경우, [자습서의 해당 부분](https://docs.aws.amazon.com/ko_kr/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html#tutorials-ecs-ecr-codedeploy-deployment)을 참고하여 새로 만들어준 후 선택해줍니다.
  <img width="568" alt="16 - 배포 그룹 설정 1" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/ffc16eea-da2d-4aee-acd8-bf7d2e65de95">
  <img width="559" alt="17 - 배포 그룹 설정 2" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/27ead182-a03b-4390-941a-d6ad63afc35d">

<br>

### 5. CodePipeline 생성 - 소스 스테이지

- 현재의 스프링부트 프로젝트를 로컬에 클론한 후, 각자의 깃허브 리포지토리에 푸시해 놓습니다.
- CodePipeline 콘솔에 접속하여 파이프라인 생성을 클릭합니다.
- 파이프라인 설정을 아래의 이미지와 같이 셋팅해준 후 '다음'을 클릭합니다.
- '서비스 역할' 부분에서 예전에 파이프라인을 만든 적이 있다면 해당 Role을 선택하고, 없다면, 'AWS CodePipeline이 새 파이프라인에 사용할 서비스 역할을 생성하도록 허용'이라는 부분에 체크를 해 줍니다.
  <img width="726" alt="18 - 파이프라인 설정 1" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/1fd08df5-f8db-443b-a5e4-b88b8a2e92c2">
- 소스 스테이지 설정에 진입한 후, CodePipeline에서 깃허브에 연결할 수 있도록 설정해줍니다.
  <img width="917" alt="19 - 파이프라인 설정 2 깃헙 연결" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/f8e4aafc-82da-4cb4-95df-ad0d2ab5c0de">
- 깃허브 연결 애플리케이션이 이미 있다면 해당 앱을 선택하고, 없다면 아래의 과정을 거쳐서 새로 만들어줍니다.
- AWS와 깃허브 리포지토리의 연결이 완료되었다면, 배포 대상 브랜치를 정확하게 선택해줍니다.
  <img width="868" alt="20 - 파이프라인 설정 2 깃헙 연결 앱 선택 없다면 새 앱 설치" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/625a7b69-56a2-436e-bcdd-9535f3378eff">
  <img width="726" alt="21 - 파이프라인 설정 2 깃헙 리포지토리와 브랜치 연동 완료" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/1a74ca5a-1bc3-493a-b2af-7fce6a737b8f">
- '다음'을 눌러서 빌드 스테이지로 넘어갑니다.

<br>

### 6. CodePipeline 생성 - 빌드 스테이지

- 빌드 스테이지 구성 화면에서 우선 '프로젝트 생성'을 클릭하여 AWS CodeBuild 내의 build project 생성 화면에 진입합니다.
- '환경 이미지'는 ubuntu 또는 Amazon Linux로 선택합니다.
- '역할 ARN'은 기존에 만들어 둔 것이 있을 경우 그것을 선택하고, 없을 경우 '새 서비스 역할'을 선택합니다.
- Buildspec 파트에서 'buildspec 파일 사용'을 클릭해줍니다.
- 본 스프링부트 프로젝트의 루트 디렉토리에는 buildspec.yml 파일이 들어 있는데, 바로 이 파일을 사용하여 AWS CodeBuild가 배포용 도커 이미지를 빌드하게 됩니다.
- 'BuildSpec 이름 - 선택사항' 부분은 buildspec.yml 파일의 이름을 적어줘야 합니다. 프로젝트 루트 디렉토리에 있을 경우 그냥 buildspec.yml 이라고 적어주면 되지만, 예를 들어서 프로젝트 내 abc 디렉토리 내부에 있다면 abc/buildspec.yml 이런 식으로 디렉토리를 정확히 적어줘야 합니다.
- 빌드 작업의 로그를 보고 싶다면 S3의 별도 버킷에 로그를 저장하도록 설정해줄 수 있습니다.
  <img width="570" alt="22 - 파이프라인 빌드 설정 1" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/6a654511-bb6d-4868-9f4c-85df81626e4a">
  <img width="530" alt="23 - 파이프라인 빌드 설정 2" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/a7f3e1cb-1c8e-4409-9a48-ded6e5bc0eff">
- 빌드 프로젝트 생성이 완료 되었다면 아래의 이미지와 같이 빌드 프로젝트 생성 완료 메시지를 확인할 수 있습니다.
  <img width="555" alt="40 - 빌드 프로젝트 생성 완료 이미지" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/a59b29f2-63c9-41b6-a332-d54a782ec0f1">
- 그 후 입역 아티팩트에서 SourceArtifact를 선택해주고, 환경 변수들을 주입해줍니다.
- 현재 스프링부트 프로젝트의 [buildspec.yml](https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/blob/master/buildspec.yml) 파일을 보면 ${...}로 표시된 텍스트들이 있는데, CodeBuild 가 빌드 작업을 진행하면서 사용하는 명령어들을 구성하는 텍스트들입니다.
- 이 텍스트들에 값을 주입해주기 위한 것입니다.
```yaml
version: 0.2

phases:
  build:
    commands:
      # 기존에 존재하는 빈 yaml파일을 삭제한다.
      - rm src/main/resources/application.yaml
      # 아래의 명령어를 통해서 s3의 dongvin-env-file라는 버킷 내의 files 라는 디렉토리 내의 application.yaml을 다운로드 받을 수 있게 된다.
      - aws s3api get-object --bucket dongvin-env-files --key files/application.yaml application.yaml
      #여기서는 다운로드 받은 야믈 파일을 적절한 디렉토리 내부에 삽입시켜준다.
      - cp -r application.yaml src/main/resources
      - rm application.yaml
      - ls src/main/resources
      - aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com
      - docker build -t ${IMAGE_REPO_URI}:latest .
      - docker tag ${IMAGE_REPO_URI}:latest ${IMAGE_REPO_URI}:${IMAGE_TAG}
      - docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}:latest
      - printf '{"ImageURI":"%s"}' ${IMAGE_REPO_URI}:${IMAGE_TAG} > imageDetail.json
      - cat imageDetail.json
      - cat scripts/taskdef.json | sed -e "s|<AWS_ACCOUNT_ID>|$AWS_ACCOUNT_ID|g" > taskdef.json
      - cat scripts/appspec.yaml > appspec.yaml

artifacts:
  files:
    - imageDetail.json
    - taskdef.json
    - appspec.yaml
```
- 환경변수들을 아래의 이미지와 같이 5 가지를 주입해줍니다.
- AWS_DEFAULT_REGION 은 현재 만들고 있는 파이프라인이 존재하는 리전의 이름을,
- AWS_ACCOUNT_ID는 루트 계정의 아이디 12자리 숫자를,
- IMAGE_REPO_URI는 앞 단계에서 만든 ECR 리포지토리의 URI를,
- IMAGE_TAG는 latest를,
- ECR_REPOSITORY_NAME은 앞 단계에서 만든 ECR 리포지토리의 이름을 입력하고, 5 가지 환경변수의 유형은 전부 '일반 텍스트'로 선택해줍니다.
  <img width="672" alt="27 - 파이프라인 빌드 단계에서 빼먹었던 환경 변수 추가" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/b13c49bb-fe87-450c-94b6-58db07407ff9">
- 그 후, '다음'을 눌러서 배포 스테이지로 넘어갑니다.

<br>

### 7. CodePipeline 생성 - 배포 스테이지

- '배포 공급자'를 Amazon ECS(Blue/Green)으로 정확하게 선택해줍니다.
- 그 후, 앞 단계에서 만들어둔 CodeDeploy Application과 배포 그룹을 선택해줍니다.
- Amazon ECS 작업 정의 부분과 AWS CodeDeploy AppSpec 파일 부분은 아래의 이미지와 같이 선택 또는 입력해줍니다.
- '이미지 세부 정보가 있는 입력 아티팩트'는 BuildArtifact를 선택해주고,
- 작업 정의의 자리 표시자 텍스트는 아래의 [taskdef.json](https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/blob/master/scripts/taskdef.json)파일의 image 필드의 값인 IMAGE1_NAME과 동일하게 입력해줍니다. 반드시 서로 일치해야 배포가 성공합니다.
```json
{
  "executionRoleArn": "arn:aws:iam::846906946119:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "sample-website",
      "image": "<IMAGE1_NAME>",
      "essential": true,
      "portMappings": [
        {
          "hostPort": 80,
          "protocol": "tcp",
          "containerPort": 80
        }
      ]
    }
  ],
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "networkMode": "awsvpc",
  "cpu": "256",
  "memory": "512",
  "family": "ecs-demo"
}
```
<img width="568" alt="28 - 파이프라인 배포 단계 만들 때, ECS BlueGreen 선택하고 자리 정의의 자리 표시자 텍스트 잘 입력하기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/052cc70c-6474-4a31-b15f-5d2040e68260">

### 8. AWS CodePipeline 완성 & CI/CD 작동 테스트

- 파이프라인이 최초로 생성되면, 그 직후 파이프라인이 작동을 시작합니다.
  <img width="634" alt="29 - 파이프라인 실행중" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/f71809de-fcc3-4298-8a6f-197c0a30e4ab">
- 소스 & 빌드 단계 성공 후 배포 단계가 실행 중일 때, 배포 단계의 작업 실행 세부 정보를 보면, 'CodeDeployToEcs에서 보기'를 선택합니다.
- 그러면 현재 진행 중인 배포 스테이지를 더 자세하게 모니터링 할 수 있습니다.
  <img width="598" alt="30 - 파이프라인 배포 단계 세부 정보 보기 선택 후 CodeDeployToEcs 에서 보기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/c5590d16-65ef-4ee3-8a87-0aab5f0edebc">
  <img width="633" alt="31 - 배포 완료 후 대기" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/fbb527c2-814f-408f-9351-e95058db82d3">
- 첫 번째 배포가 완료된 후, Postman API 테스터로 컨테이너가 잘 작동하는지 테스트 합니다.
  <img width="588" alt="32 - 배포 완료 후 ALB DNS로 테스트 성공" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/3ac534ff-e502-40f6-9d87-e1930cc9a1dd">
- 배포가 완료되면 아래와 같은 이미지를 확인할 수 있습니다.
  <img width="619" alt="33 - 배포 완료 후 코드디플로이 작업 상태" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/4845364e-f730-4838-969f-96666edeb278">
- 로컬 스프링부트 프로젝트에 새로운 커밋을 추가하고 원격 리포지토리에 푸시합니다.
  <img width="843" alt="34 - 스프링 프로젝트에 새 깃허브 커밋 추가" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/620986c3-46d9-490f-9691-de0168cbd157">
- 푸시 후 몇 초 지나지 않아서 AWS CodePipeline으로 만든 CI/CD 파이프라인이 작동을 시작합니다.
  <img width="648" alt="35 - 새 커밋 푸시 후 파이프라인 자동 작동" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/82c33f99-99e1-48c0-be98-cc2b65dca87b">
- 새로운 컨테이너가 트래픽을 받는 중이고, 이때 Postman을 이용해서 새로운 커밋이 잘 배포 됐는지 확인합니다.
  <img width="618" alt="36 - 새 배포 후 새로운 인스턴스가 트래픽 담당하는 모습 확인" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/46c7781a-c33e-4788-9432-401b6a42df39">
- CI/CD 파이프라인에 의해서 새로운 커밋이 잘 배포 됐음을 확인할 수 있습니다!
  <img width="584" alt="37 - 새 배포 후 ALB DNS로 트래픽 라우팅 성공" src="https://github.com/DongvinPark/AWS-Code-Pipe-Line-Test/assets/99060708/f63f19da-bc1d-4a20-90d2-9975a0fcff9d">












