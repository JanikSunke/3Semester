docker build -t gitlab.sdu.dk:5050/semester-project-e2021/team-04/subscription:subscription  -f SubDockerfile.DEV .
docker build -t gitlab.sdu.dk:5050/semester-project-e2021/team-04/subscription:mail -f MailDockerfile.DEV .
docker build -t gitlab.sdu.dk:5050/semester-project-e2021/team-04/subscription:mockup -f MockupDockerfile .