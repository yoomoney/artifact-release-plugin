# artifact-release-plugin

Плагин создан для релиза библиотек и артефактов (не обязательно jar) не привязан к конкретному языку программирована/платформе, может быть использован как инструмент для релиза java библиотек, так и спецификаций в формате OpenApi.

Перед использованием этого плагина нужно принять несколько договоренностей:
* проект использует git в качестве системы контроля версий
* версия проекта находится в файле ```gradle.properties``` в корне репозитория, в формате ```version=x.y.z-SNAPSHOT```
* решение о релизе принимается вызывающим (ci-cd/разработчиком), т.е. плагин не проверят в какой ветке вы находитесь, был ли уже релиз с такой версий и т.д.

### Устройство плагина: 

Существует несколько основных задачи

1. ```preRelease``` - задача убирает из ```version``` которая находится в ```gradle.properties``` постфикс ```-SNAPSHOT```,   если в проекте есть ```CHANGELOG.md``` следующая версия берется оттуда, сам ```CHANGELOG.md``` обновляется исходя из заполненных маркеров,  делается коммит и добавляется tag в git с новой версий    
1. ```release``` - задача перед которой поочередно выполняются задачи для релиза определенные пользователем через расширение ```releaseSettings``` затем увеличивается patch версия в ```gradle.properties``` и  к ней добавляется постфикс ```-SNAPSHOT``` если в проекте есть ```CHANGELOG.md``` добавляется маркеры для описания следующих версий, делается commit и push с новой версией
1. ```checkChangelog``` - задача проверяет правильно ли заполнено описание следующего релиза в ```CHANGELOG.md```

Задачи ```preRelease``` и ```release``` должны выполнятся в 2 разных gradle процессах, это важно т.к. только на момент инициализации gradle берет версию из ```gradle.properties```, далее другие плагина на нее ориентируются в фазе afterEvaluate, т.е. по ходу выполнения gradle задач изменить версию проекта поменять нельзя.

Вызывающий сам определяет нужен ли релиз исходя из последнего коммита, это сделано потому, что в gradle нет хороших механизмов прервать выполнения текущих задач и оставить gradle сборку успешной.
В общем виде пример конфигурации для jenkins pipeline для мастер ветки:
```groovy
...
    stage('release') {
        when {                  
            expression {                
                return !sh(script: "git log -1 --pretty=%B", returnStdout: true).trim().startsWith("[Gradle Release Plugin]")
            }                
        }
        steps {
            sh './gradlew preRelease'                
            sh './gradlew release'                
        }                        
    }
...    
```


### Пример конфигурации плагина:
```groovy
releaseSettings {
    /**
    * Перечень задач которые будут выполнены в момент релиза  
    */
    releaseTasks = ['build', 'buildDeb', 'uploadDebToNexus']    
    /**
     * Перечень задач которые нужно выполнить до релиза, 
     * версию и changelog будущего артефакта можно получить через `ReleaseInfoStorage(project.buildDir)` 
     */
    preReleaseTasks = ['updateArtifactBeforeRelease']
    
    /**
     *  Путь до приватного ssh ключа для дотсупа в git, если задан будет использоваться
     */
    pathToGitPrivateSshKey = null

    /**
     *  Passphrase для приватного ssh ключа. По умолчанию не задана, имеет смысл совместно с pathToGitPrivateSshKey
     */
    sshKeyPassphrase = null
    
    /**
     *  Имя юзера, от которого будет производиться коммит в git. Обязательная настройка.
     */
    gitUsername = 'user'
    
    /**
    *  Email юзера, от которого будет производиться коммит в git. Обязательная настройка.
    */
    gitEmail = 'user@mail.ru'
        
    /**
     *  Требовать наличия файла CHANGELOG.md в корне проекта
     */
    changelogRequired = true
}
```

Задачи описанные в `preReleaseTasks` будут выполнени после обновления версии в ```gradle.properties``` и ротации ```CHANGELOG.md``` но до коммита всех изменений в git c сообщением `[Gradle Release Plugin] - pre tag commit` версию и changelog текущего артефакта можно получить через `ReleaseInfoStorage(project.buildDir)`

Некоторые артифакты требуют правок в момент поднятии версии, например спецификация в формате OpenAPI v3. Версии такой спецификации хранится в секции info:version внутри yml файла. 
Для того что бы поддержать возможность правки артифакдо в момент поднятия версии но до первого комита добавлена секция `preReleaseTasks`  

### Как вычисляется версия релиза:

Поддерживается 2 варианта работы:
1. Если в проекте нет ```CHANGELOG.md```, то значение релизной версии берется из ```version``` в ```gradle.properties``` путем отбрасывания постфикса ```-SNAPSHOT```, после релиза поднимается patch версия и к ней добавляется ```-SNAPSHOT```   новое значение записывается в ```gradle.properties```
1. Если в проекте есть ```CHANGELOG.md```, то значение релизной версии берется из маркера ```### NEXT_VERSION_TYPE=???``` путем вычисления на основе предыдущей версии из ```CHANGELOG.md```  (если это первый релиз и в ```CHANGELOG.md``` нет информации о предыдущей версии, релизная версия будет ```0.0.1``` или ```0.1.0``` или ```1.0.0``` соответственно) , после релиза поднимается patch версия и к ней добавляется ```-SNAPSHOT```, новое значение записывается в ```gradle.properties```

### Содержание важных для релиза файлов на всех этапах разработки:
1. От мастера отведена ```feature``` ветка, разработчик собирается внести изменения       
   ```
   gradle.properties
   version=1.0.1-SNAPSHOT
   ```
   ```
   CHANGELOG.md
   ### NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH
   ### NEXT_VERSION_DESCRIPTION_BEGIN
   ### NEXT_VERSION_DESCRIPTION_END
   ## [1.0.0]() (30-05-1992)
   
   some description
   ```
1. Разработчик внес изменения в проект, сделал пулл реквест 
   ```
   gradle.properties
   version=1.0.1-SNAPSHOT
    ```
   ```
   CHANGELOG.md
   ### NEXT_VERSION_TYPE=MINOR
   ### NEXT_VERSION_DESCRIPTION_BEGIN
   Добавлен функционал Х
   ### NEXT_VERSION_DESCRIPTION_END
   ## [1.0.0]() (30-05-1992)
   
   some description
   ```
   в ```NEXT_VERSION_TYPE``` указал ```MINOR``` т.к. был добавлен новый функционал, описал сделанные изменения в  ```NEXT_VERSION_DESCRIPTION``` 
1. Пулл реквест замержен в master начинается сборка релиза, после ```preRelese``` состояние такое
   ```
   gradle.properties
   version=1.1.0
   ```
   ```
   CHANGELOG.md
   ## [1.1.0]() (30-12-2018)
    
   Добавлен функционал Х
 
   ## [1.0.0]() (30-05-1992)
    
   some description
   ``` 
   дата ```30-12-2018``` вычисляется на основе текущего дня, все задачи, описанные в ```releaseTasks``` будут выполнены сейчас
1. После релиза изменения будут запушены в гит
   ```
   gradle.properties
   version=1.1.1-SNAPSHOT
   ```
   ```
   CHANGELOG.md
   ### NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH
   ### NEXT_VERSION_DESCRIPTION_BEGIN
   ### NEXT_VERSION_DESCRIPTION_END   
   ## [1.1.0]() (30-12-2018)
   
   Добавлен функционал Х
 
   ## [1.0.0]() (30-05-1992)
    
   some description
   ```     
   в гит добавлен тег и 2 коммита 
   ```
   * 38bcac4 - (HEAD) [Gradle Release Plugin] - new version commit: '1.1.1-SNAPSHOT'. 
   * 1de1fa5 - (tag: refs/tags/1.1.0) [Gradle Release Plugin] - pre tag commit: '1.1.0'. 
   ```   