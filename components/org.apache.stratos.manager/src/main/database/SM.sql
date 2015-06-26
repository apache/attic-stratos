SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE SCHEMA IF NOT EXISTS `StratosManager` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `StratosManager` ;

-- -----------------------------------------------------
-- Table `StratosManager`.`Cluster`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `StratosManager`.`Cluster` (
  `clusterId` VARCHAR(255) NULL,
  `alias` VARCHAR(255) NULL,
  `serviceName` VARCHAR(255) NULL,
  `tenantRange` VARCHAR(255) NULL,
  `isLbCluster` VARCHAR(255) NULL,
  PRIMARY KEY (`clusterId`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `StratosManager`.`DomainMapping`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `StratosManager`.`DomainMapping` (
  `domainName` VARCHAR(255) NOT NULL,
  `serviceName` VARCHAR(255) NULL,
  `contextPath` VARCHAR(255) NULL,
  PRIMARY KEY (`domainName`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `StratosManager`.`ArtifactRepository`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `StratosManager`.`ArtifactRepository` (
  `alias` VARCHAR(255) NOT NULL,
  `isPrivateRepo` TINYINT(1) NULL,
  `repoUserName` VARCHAR(255) NULL,
  `repoPassword` VARCHAR(255) NULL,
  `repoURL` VARCHAR(255) NULL,
  `catridgeType` VARCHAR(255) NULL,
  PRIMARY KEY (`alias`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `StratosManager`.`ApplicationSignUp`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `StratosManager`.`ApplicationSignUp` (
  `applicationId` VARCHAR(255) NOT NULL,
  `tenantId` INT NOT NULL,
  `Cluster_clusterId` VARCHAR(255) NOT NULL,
  `DomainMapping_domainName` VARCHAR(255) NOT NULL,
  `ArtifactRepository_alias` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`applicationId`, `tenantId`, `Cluster_clusterId`, `DomainMapping_domainName`, `ArtifactRepository_alias`),
  INDEX `fk_ApplicationSignUp_Cluster_idx` (`Cluster_clusterId` ASC),
  INDEX `fk_ApplicationSignUp_DomainMapping1_idx` (`DomainMapping_domainName` ASC),
  INDEX `fk_ApplicationSignUp_ArtifactRepository1_idx` (`ArtifactRepository_alias` ASC),
  CONSTRAINT `fk_ApplicationSignUp_Cluster`
    FOREIGN KEY (`Cluster_clusterId`)
    REFERENCES `StratosManager`.`Cluster` (`clusterId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ApplicationSignUp_DomainMapping1`
    FOREIGN KEY (`DomainMapping_domainName`)
    REFERENCES `StratosManager`.`DomainMapping` (`domainName`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ApplicationSignUp_ArtifactRepository1`
    FOREIGN KEY (`ArtifactRepository_alias`)
    REFERENCES `StratosManager`.`ArtifactRepository` (`alias`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
