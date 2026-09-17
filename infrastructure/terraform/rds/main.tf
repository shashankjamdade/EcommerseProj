variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "db_instance_class" {
  type = string
}

variable "allocated_storage" {
  type = number
}

variable "postgres_version" {
  type = string
}

variable "master_username" {
  type      = string
  sensitive = true
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}

resource "aws_db_instance" "main" {
  identifier              = "${var.project_name}-db"
  engine                  = "postgres"
  engine_version          = var.postgres_version
  instance_class          = var.db_instance_class
  allocated_storage       = var.allocated_storage
  db_name                 = "maindb"
  username                = var.master_username
  password                = random_password.db_password.result
  db_subnet_group_name    = aws_db_subnet_group.main.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  skip_final_snapshot     = var.environment != "prod"
  publicly_accessible     = false
  backup_retention_period = 7

  tags = {
    Name = "${var.project_name}-db"
  }
}

output "db_endpoint" {
  value = aws_db_instance.main.endpoint
}

output "db_password" {
  value     = random_password.db_password.result
  sensitive = true
}

