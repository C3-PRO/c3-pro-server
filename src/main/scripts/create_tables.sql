CREATE TABLE Users(username VARCHAR2(255), passwd VARCHAR2(255), PRIMARY KEY (username));
CREATE TABLE UserRoles(username VARCHAR2(255), userRoles VARCHAR2(255));
CREATE TABLE UserTokens(username VARCHAR2(255), token VARCHAR2(255), expirationDate DATE, PRIMARY KEY(token));