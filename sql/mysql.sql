CREATE DATABASE `xref` /*!40100 DEFAULT CHARACTER SET latin1 */;

CREATE TABLE IF NOT EXISTS `entitytype` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `entitytype` VARCHAR(45) NOT NULL,
  `tenant` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `ENTITYTYPE` (`entitytype` ASC),
  INDEX `TENANT` (`tenant` ASC, `entitytype` ASC))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `relation` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `commonid` VARCHAR(45) NOT NULL,
  `entitytype_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `COMMONID` (`commonid` ASC),
  INDEX `fk_relation_entitytype1_idx` (`entitytype_id` ASC),
  CONSTRAINT `fk_relation_entitytype1`
    FOREIGN KEY (`entitytype_id`)
    REFERENCES `entitytype` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `reference` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `relation_id` INT NOT NULL,
  `endpoint` VARCHAR(45) NOT NULL,
  `endpointid` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_reference_relation_idx` (`relation_id` ASC),
  INDEX `ENDPOINT` (`endpoint` ASC, `endpointid` ASC),
  CONSTRAINT `fk_reference_relation`
    FOREIGN KEY (`relation_id`)
    REFERENCES `relation` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;