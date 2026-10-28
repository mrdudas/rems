-- EPKEB: the approvals (permits) issued for approved applications, see
-- docs/epkeb/virtualis-engedely.md
CREATE TABLE permit (
  id SERIAL PRIMARY KEY,
  -- the secret part of the public verification link
  token VARCHAR(64) NOT NULL UNIQUE,
  -- the reference number is year-sequence, e.g. 2026-077
  year INTEGER NOT NULL,
  sequence INTEGER NOT NULL,
  appId INTEGER NOT NULL UNIQUE REFERENCES catalogue_item_application (id),
  issued TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked TIMESTAMP WITH TIME ZONE,
  -- the data of the approval at the time of issuing
  data JSONB NOT NULL,
  UNIQUE (year, sequence)
);
