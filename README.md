# serverMobApp
server code is necessary to run MobApp project.

( Add MySQL connector to your project library ( https://dev.mysql.com/downloads/connector/j/5.1.html ) .
Connecting to server can cause some problems when uploading files larger than approx 1MB. Change in the my.ini file by including the single line under [mysqld] section in your file: max_allowed_packet=500M )
