### NEXT_VERSION_TYPE=MAJOR
### NEXT_VERSION_DESCRIPTION_BEGIN
* **breaking changes** Взаимодействие с bitbucket переведено на apiToken. Для перехода на версию необходимо: 
  1. выпустить apiToken и прописать его в настройку releaseSettings.bitbucketApiToken;
  1. удалить настройки releaseSettings.bitbucketUser и releaseSettings.bitbucketPassword.
### NEXT_VERSION_DESCRIPTION_END
## [3.11.2](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/17) (20-04-2021)

* Снижено количество detekt нарушений 41 -> 18.

## [3.11.1](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/16) (30-03-2021)

* `В CheckChangeLogTask` добавлено условие, что наличие **breaking changes** при мажорном обновлении может отсутствовать для версии 1.0.0

## [3.11.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/15) (26-03-2021)

* `В CheckChangeLogTask` добавлена проверка на то, что присутствует секция **breaking changes** при мажорном обновлении плагина.
То есть, если изменения мажорные (MAJOR), то в CHANGELOG.MD, в описании изменений, обязательно требуется секция **breaking changes**.

## [3.10.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/14) (18-03-2021)

* Сборка проекта переведена на gradle-project-plugin.

## [3.9.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/13) (02-03-2021)

* Подключен java-artifact-publish-plugin. Теперь публикация осуществляется в MavenCentral.

## [3.8.1](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/12) (04-02-2021)

* Понижена версия зависимостей kotlin по причине проблем при сборке на windows: https://youtrack.jetbrains.com/issue/KT-26513

## [3.8.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/11) (04-02-2021)

* Удален ключ git_key.enc в связи с изменением логики работы с git_key.
Подробности см. https://github.com/yoomoney-gradle-plugins/travis-shared-configuration/pull/8
* Поднята версия artifact-release-plugin

## [3.7.0](https://api.github.com/repos/yoomoney-gradle-plugins/artifact-release-plugin/pulls/9) (03-02-2021)

* Поднята версия artifact-release-plugin

## [3.6.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/7) (22-01-2021)

* Конфигурация из travis.yml вынесена в отдельный репозиторий yoomoney-gradle-plugins/travis-shared-configuration.
* Поднята версия подключаемого artifact-release-plugin 3.4.0 -> 3.5.1
* Поправлен парсинг пути к репозиторию для получения ссылки на pull-request

## [3.5.1]() (18-01-2021)

* Удалено условие выполнение таски bintrayUpload только для неснапшотных версий, т.к. теперь таска запускается только на мастер ветке.
* Исправлен парсинг строки с репозиторием в PullRequestLinkProvider. Теперь корректно работает и с http и c ssh урлами.

## [3.5.0]() (15-01-2021)

* Подключен artifact-release-plugin для автоматического выпуска релиза.
* В travis.yml добавлена стадия выпуска релиза, которая запускается только на master ветке.
* Добавлен зашифрованный shh-key для git.

## [3.4.0](https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/pull/1) (29-12-2020)
  
* Внесены изменения в связи с переходом в GitHub:
    * Переименованы пакеты
    * Плагин собирается без использования project-plugin, сборка полностью описывается в build.gradle
    * Сборка переведена на travis (ранее использовался jenkins)
    * Добавлена настройка pullRequestInfoProvider для выбора провайдера ссылок на PR (gitHub/bitbucket)

## [3.3.2]() (30-11-2020)

* Обновлена версия kotlin 1.3.71 -> 1.3.50

## [3.3.1]() (23-11-2020)

* Замена доменов email @yamoney.ru -> @yoomoney.ru

## [3.3.0]() (20-11-2020)

* Поднята версия gradle: 6.0.1 -> 6.4.1.

## [3.2.0]() (27-02-2020)

* Добавление ссылки на bitbucket pull request при ротации changelog.md

## [3.1.0]() (05-02-2020)

* Сборка на java 11

## [3.0.1]() (30-01-2020)

* Удален snapshots репозиторий.

## [3.0.0]() (29-01-2020)

* Обновлена версия gradle `4.10.2` -> `6.0.1`
* Обновлены версии зависимостей
* Исправлены warnings и checkstyle проблемы

## [2.1.0]() (29-11-2019)

* Добавлен утилитарный класс ArtifactVersionProvider умеющий определять версию текущего артефакта

## [2.0.1]() (05-07-2019)

* Обновлена версия yamoney-git-client=2.0.0 -> 2.1.0,
для исправления автора коммита при локальной сборке

## [2.0.0]() (28-06-2019)

* Работа с гитом переведена на библиотеку git-client
* **breaking changes** Для git появились обязательные настройки - email и username,
от имени которых будет производиться коммит. Пример настроек, которые необходимо добавить:
```
releaseSettings {
gitEmail = 'user@mail.ru'
gitUsername = 'user'
}
```

## [1.4.5]() (27-06-2019)

* Исправлен метод проверки существования тега в git

## [1.4.4]() (27-05-2019)

* Добавлена поддержка passphrase для ssh ключа

## [1.4.3]() (22-05-2019)

* Сборка переведена на yamoney-gradle-project-plugin=5.+

## [1.4.2]() (14-05-2019)

* Добавлен репозиторий с Gradle плагинами

## [1.4.1]() (05-04-2019)

* Исправлен метод проверки существования тега в git.

## [1.4.0]() (29-03-2019)

* Добавлены проверки на стадии PreRelease:
1) проверка существования незакоммиченных изменений
2) проверка прав доступа к git - пушим пустой коммит, который должен быть успешен
3) проверка отсутствия релизного тега для версии будущего релиза

## [1.3.0]() (29-03-2019)

* preRelease комитит новые файлы если такие есть

## [1.2.3]() (20-03-2019)

* Исправлена авторизация в гите по ssh

## [1.2.2]() (04-03-2019)

* Исправлена авторизация в гите по ssh

## [1.2.1]() (04-03-2019)

* Поменялся формат маркеров в chagnelog, сборка в фича ветках может упасть
поправьте %% на ### в changelog.md

## [1.2.0]() (26-02-2019)

* Переход на platformGradleProjectVersion 4 версии

## [1.1.1]() (25-02-2019)
* preReleaseCheckExecuted должен зависить от release, а не от первой задачи в releaseExtension.releaseTasks

## [1.1.0]() (18-02-2019)
* задача release требует что бы перед ней был выполнен preRelease

## [1.0.0]() (29-01-2019)
* начальная версия.
* добавлена возможность указвывать приватный ssh ключ при пуше
* добавлена возможность требовать CHANGELOG.md при сборке
* при релизе сохраняется версия и описание релиза 
* исправлен `push` после релиза
* обновлен `README.md`
* У `release` плагина появилась секция `preReleaseTasks`