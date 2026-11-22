// 职考云 CI：Gogs 拉代码 → Maven 打包 → 按服务 docker build + run → 冒烟
// 部署方式仿照天机学堂：每个服务由 docker/startup.sh 拷贝 jar、构建镜像、跑容器
//（原版是 tj-xxx job 里 ssh 到 VM 执行 /usr/local/src/script/startup.sh，这里 Jenkins 本身就在 VM 上，直接执行）
pipeline {
  agent any

  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '15'))
    timeout(time: 60, unit: 'MINUTES')
  }

  parameters {
    string(name: 'SERVICES', defaultValue: '', description: '留空=全部 15 个服务；也可填 gateway,auth,user 只部署部分')
    booleanParam(name: 'SKIP_TESTS', defaultValue: true, description: '跳过单元测试')
  }

  environment {
    MVN = '/var/jenkins_home/tools/apache-maven-3.8.6/bin/mvn'
    MVN_SETTINGS = '/var/jenkins_home/.m2/settings.xml'
    MVN_REPO = '/var/jenkins_home/.m2/repository'
    MAVEN_OPTS = '-Xmx512m'
    NETWORK = 'zkxt-net'
    PROBE_HOST = '192.168.150.101'
  }

  stages {
    stage('拉取代码') {
      steps {
        checkout scm
        sh 'echo "commit: $(git rev-parse --short HEAD)"; git log -1 --pretty="%h %s"'
      }
    }

    stage('编译打包') {
      steps {
        sh '''
          set -e
          "$MVN" -B -T 1C -s "$MVN_SETTINGS" -Dmaven.repo.local="$MVN_REPO" -DskipTests package
          echo "---- 产物 ----"
          find . -maxdepth 3 -path "*/target/*.jar" -not -name "*sources*" | wc -l
        '''
      }
    }

    stage('构建镜像并部署') {
      steps {
        sh '''
          set -e
          chmod +x docker/*.sh
          if [ -n "$SERVICES" ]; then
            SERVICES="$(echo "$SERVICES" | tr ',' ' ')" bash docker/deploy.sh
          else
            bash docker/deploy.sh
          fi
        '''
      }
    }

    stage('冒烟验证') {
      steps {
        sh 'bash docker/smoke.sh'
      }
    }
  }

  post {
    success {
      sh 'docker ps --filter "name=zkxt-" --format "{{.Names}} {{.Status}}" || true'
    }
    failure {
      sh '''
        for c in $(docker ps -a --filter "name=zkxt-" --format "{{.Names}}"); do
          echo "==== $c"; docker logs --tail 25 "$c" 2>&1 || true
        done
      '''
    }
  }
}
