%% NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH
%% NEXT_VERSION_DESCRIPTION_BEGIN
%% NEXT_VERSION_DESCRIPTION_END
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