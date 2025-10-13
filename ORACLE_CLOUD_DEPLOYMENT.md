# Oracle Cloud Always Free Deployment Guide

## Why Oracle Cloud Always Free?
- ♾️ **Permanent free tier** (no 12-month limit like AWS)
- 🚀 **2 AMD instances** (1 GB RAM each)
- 💾 **200 GB storage** 
- 🌐 **10 TB bandwidth/month**
- 🆓 **Completely free forever**

## Step 1: Create Oracle Cloud Account

### 1.1 Sign Up
1. Go to [cloud.oracle.com](https://cloud.oracle.com)
2. Click **"Start for free"**
3. Choose **"Always Free"** option
4. Verify with credit card (no charges for free tier)
5. Wait for account activation (can take 15-30 minutes)

### 1.2 Access Console
1. Login to Oracle Cloud Console
2. Select your **Home Region** (choose closest to you)
3. Navigate to **Compute** → **Instances**

## Step 2: Create Compute Instance

### 2.1 Launch Instance
1. Click **"Create Instance"**
2. **Name**: `ecommerce-app`
3. **Image**: **"Oracle Linux 8"** (free)
4. **Shape**: **"VM.Standard.E2.1.Micro"** (Always Free eligible)
5. **VCN**: Create new VCN (accept defaults)
6. **Subnet**: Public subnet
7. **SSH Keys**: Generate new key pair (download .key file)

### 2.2 Configure Security Lists
After instance creation, configure security rules:

```bash
# Go to Networking → Virtual Cloud Networks → Your VCN → Security Lists
# Add these ingress rules:

Rule 1:
- Source: 0.0.0.0/0
- IP Protocol: TCP
- Destination Port Range: 22 (SSH)

Rule 2:
- Source: 0.0.0.0/0  
- IP Protocol: TCP
- Destination Port Range: 8080 (Your App)

Rule 3:
- Source: 0.0.0.0/0
- IP Protocol: TCP
- Destination Port Range: 80 (HTTP)

Rule 4:
- Source: 0.0.0.0.0
- IP Protocol: TCP
- Destination Port Range: 443 (HTTPS)
```

## Step 3: Connect and Setup

### 3.1 Connect to Instance
```bash
# Connect via SSH
ssh -i "your-key.key" opc@your-instance-ip

# Update system
sudo dnf update -y

# Install Java 21
sudo dnf install -y java-21-openjdk java-21-openjdk-devel

# Install Docker
sudo dnf install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker opc

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Git
sudo dnf install -y git

# Logout and login for Docker group
exit
ssh -i "your-key.key" opc@your-instance-ip
```

### 3.2 Install Additional Tools
```bash
# Install useful tools
sudo dnf install -y htop nano curl wget

# Install Gradle (for building)
wget https://services.gradle.org/distributions/gradle-8.10.2-bin.zip
sudo unzip gradle-8.10.2-bin.zip -d /opt/
sudo ln -s /opt/gradle-8.10.2/bin/gradle /usr/bin/gradle

# Verify installations
java -version
docker --version
gradle --version
```

## Step 4: Deploy Your Application

### 4.1 Clone and Build
```bash
# Clone your repository
git clone https://github.com/b-rine/ecommerce-stream.git
cd ecommerce-stream

# Make gradlew executable
chmod +x gradlew

# Build the application
./gradlew build -x test
```

### 4.2 Run with Docker Compose
```bash
# Start with Docker Compose (includes Kafka)
docker-compose up -d

# Check if containers are running
docker ps

# View logs
docker-compose logs -f
```

### 4.3 Alternative: Run Directly with Java
```bash
# If you prefer not to use Docker
java -jar build/libs/kafka-ecommerce-stream-0.0.1-SNAPSHOT.jar

# Or run in background
nohup java -jar build/libs/kafka-ecommerce-stream-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

## Step 5: Access Your Application

Your app will be available at:
- **HTTP**: `http://your-instance-ip:8080`
- **Dashboard**: `http://your-instance-ip:8080`
- **Logs**: `http://your-instance-ip:8080/logs`
- **Database**: `http://your-instance-ip:8080/h2-console`

## Step 6: Set Up Domain and SSL (Optional)

### 6.1 Get Free Domain
- Use **Freenom** for free domains (.tk, .ml, .ga, .cf)
- Or cheap domain from **Namecheap** (~$1/month)

### 6.2 Configure DNS
```bash
# Point your domain to your Oracle Cloud IP
# A record: your-domain.com → your-instance-ip
```

### 6.3 Install Nginx and SSL
```bash
# Install Nginx
sudo dnf install -y nginx

# Install Certbot
sudo dnf install -y certbot python3-certbot-nginx

# Configure Nginx
sudo nano /etc/nginx/conf.d/ecommerce.conf
```

**Nginx configuration:**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Get SSL certificate
sudo certbot --nginx -d your-domain.com
```

## Step 7: Set Up Second Instance (Database)

### 7.1 Create Database Instance
1. Create another **VM.Standard.E2.1.Micro** instance
2. Name: `ecommerce-database`
3. Install PostgreSQL:

```bash
# On the database instance
sudo dnf install -y postgresql15-server postgresql15
sudo postgresql-setup --initdb
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Create database and user
sudo -u postgres psql
CREATE DATABASE ecommerce;
CREATE USER appuser WITH PASSWORD 'securepassword';
GRANT ALL PRIVILEGES ON DATABASE ecommerce TO appuser;
\q
```

### 7.2 Update Application Configuration
```bash
# On your app instance, update application.yml
spring:
  datasource:
    url: jdbc:postgresql://database-instance-ip:5432/ecommerce
    username: appuser
    password: securepassword
```

## Monitoring and Maintenance

### Health Checks
```bash
# Check application status
curl http://localhost:8080/actuator/health

# Check system resources
htop
df -h
free -h

# Check running processes
ps aux | grep java
```

### Logs
```bash
# Application logs
tail -f kafka-ecommerce.log

# Docker logs
docker-compose logs -f

# System logs
sudo journalctl -f
```

### Updates
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
./gradlew build -x test
docker-compose down
docker-compose up -d
```

## Cost: $0 Forever! 🎉

```
✅ 2 AMD instances: FREE
✅ 200 GB storage: FREE
✅ 10 TB bandwidth: FREE
✅ Load balancer: FREE
✅ Total: $0/month FOREVER
```

## Advantages of Oracle Cloud Always Free

1. **♾️ Permanent** - no time limits
2. **🚀 More resources** - 2 instances vs 1
3. **💾 More storage** - 200 GB vs 30 GB
4. **🌐 More bandwidth** - 10 TB vs 15 GB
5. **🆓 Completely free** - forever
6. **🏢 Enterprise-grade** - real Oracle infrastructure

This is the best free option for learning deployment!
