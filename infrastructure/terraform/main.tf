terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "ecommerce-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Environment = var.environment
      Project     = "ecommerce-platform"
      ManagedBy   = "Terraform"
    }
  }
}

# VPC
module "vpc" {
  source = "./networking"

  project_name = var.project_name
  environment  = var.environment
  aws_region   = var.aws_region
  vpc_cidr     = var.vpc_cidr
}

# EKS
module "eks" {
  source = "./eks"

  project_name           = var.project_name
  environment            = var.environment
  aws_region             = var.aws_region
  vpc_id                 = module.vpc.vpc_id
  subnet_ids             = module.vpc.private_subnet_ids
  cluster_version        = var.eks_cluster_version
  node_group_desired     = var.eks_node_desired_size
  node_group_min         = var.eks_node_min_size
  node_group_max         = var.eks_node_max_size
  node_instance_types    = var.eks_node_instance_types
}

# RDS
module "rds" {
  source = "./rds"

  project_name         = var.project_name
  environment          = var.environment
  vpc_id               = module.vpc.vpc_id
  private_subnet_ids   = module.vpc.private_subnet_ids
  db_instance_class    = var.db_instance_class
  allocated_storage    = var.db_allocated_storage
  postgres_version     = var.postgres_version
  master_username      = var.db_master_username
}

# Redis
module "redis" {
  source = "./redis"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  node_type          = var.redis_node_type
  num_cache_nodes    = var.redis_num_nodes
}

# ECR
module "ecr" {
  source = "./ecr"

  project_name = var.project_name
  environment  = var.environment
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "rds_endpoint" {
  value = module.rds.db_endpoint
}

output "redis_endpoint" {
  value = module.redis.redis_endpoint
}

