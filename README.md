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
### 1. 

### 2. 