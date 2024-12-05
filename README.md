# Информация
Кросс серверный плагин для жалоб на читеров. Идея была взята с сервера HolyWorld.
Плагин тестировался на версии 1.16.5.

## Поддержка кросс серверности
Чтобы все работало как нужно достаточно подключить плагин на каждом из серверов к одной базе данных и к одному Redis (в конфиге).

## Команды:
* Открыть меню со списком жалоб - **/reportlist** [для модераторов]
* Перезагрузить плагин - **/reports reload** [для администрации]
* Создать жалобу - **/reports add (ник) (коммент)** [для администрации]
* Получить жалобу по номеру - **/reports get (№ жалобы)** [для администрации]
* Получить жалобы на игрока - **/reports get (ник)** [для администрации]
* Удалить жалобы по номеру - **/reports del (№ жалобы)** [для администрации]
* Начать рассмотрение жалобы по номеру - **/reports check (№ жалобы)** [для администрации]
* Временно заблокировать отправку жалоб игроку - **/rmute (ник) (срок) (причина)** [для модерации]
* Снять блокировку отправки жалоб - **/runmute (ник) (причина)** [для администрации]
* Отправить жалобу на игрока - **/cheatreport (ник) (комментарий)** [для игроков]
