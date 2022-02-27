CREATE TABLE Roles(
    id SERIAL PRIMARY KEY,
    description VARCHAR(30) NOT NULL
);

CREATE TABLE SubscriptionTiers (
	id SERIAL PRIMARY KEY,
	description VARCHAR(30) NOT NULL
);

CREATE TABLE Users (
	id SERIAL PRIMARY KEY,
	name VARCHAR(30) NOT NULL,
	password VARCHAR(50) NOT NULL,
	role INT REFERENCES Roles(id) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE families (
    id SERIAL PRIMARY KEY,
    familyId INT NOT NULL,
    userId INT REFERENCES users(id)
);

CREATE TABLE Invoices (
    id SERIAL PRIMARY KEY,
    timestamp DATE NOT NULL,
    amount BIGINT,
    userId INT REFERENCES users(id),
    subscriptionTierId INT REFERENCES subscriptiontiers(id)
);

CREATE TABLE Subscriptions (
	id SERIAL PRIMARY KEY,
	userid INT REFERENCES Users(id),
	tier INT REFERENCES SubscriptionTiers(id)
);

INSERT INTO SubscriptionTiers (description) VALUES ('Student');
INSERT INTO SubscriptionTiers (description) VALUES ('Premium');
INSERT INTO SubscriptionTiers (description) VALUES ('Family');
INSERT INTO Roles (description) VALUES ('User');
INSERT INTO Roles (description) VALUES ('Artist');
INSERT INTO Roles (description) VALUES ('Staff');

create function isfamilytier(subtier integer) returns boolean
    language plpgsql
as
$$
DECLARE
    familyTier int := subtybebyname('FAMILY');
BEGIN
    IF subTier IS NULL THEN
        return false;
    ELSEIF subTier = familyTier THEN
        return true;
    ELSE
        return false;
    END IF;
END;
$$;

alter function isfamilytier(integer) owner to subscription;

create function subtypebyname(nametofind character varying) returns integer
    language plpgsql
as
$$
DECLARE
    subId integer := 1; -- Uknown
BEGIN
    SELECT SI.id INTO suBid FROM subscriptiontiers SI WHERE upper(SI.description) = upper(nameToFind);
    RETURN subId;
END;
$$;

alter function subtybebyname(varchar) owner to subscription;

create function userhaspremiumfeatures(usertocheck character varying) returns boolean
    language plpgsql
as
$$
DECLARE
    familyTier int := subtybebyname('FAMILY');
BEGIN
    -- If the user has any subscription of his own
    IF (SELECT s.tier FROM users u
        LEFT OUTER JOIN subscriptions s on u.id = s.userid
        WHERE u.id = userToCheck LIMIT 1) IS NOT NULL THEN
        return true;
    END IF;

    -- Check for families
    IF ((SELECT COUNT(f.id) FROM users u
        INNER JOIN families f on u.id = f.userid
        LEFT OUTER JOIN subscriptions s on u.id = s.userid
        WHERE f.familyid = (SELECT familyid FROM families where userId = userToCheck) AND isFamilyTier(s.tier) LIMIT 1) > 0) THEN
        return true;
    END IF;
    return false;
END;
$$;

alter function userhaspremiumfeatures(varchar) owner to subscription;
