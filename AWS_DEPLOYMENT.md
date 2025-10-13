# AWS EC2 Deployment Guide

## Prerequisites
- AWS account (new accounts get 12 months free!)
- Basic Linux command knowledge

## Step 1: Launch EC2 Instance

### 1.1 Login to AWS Console
- Go to [aws.amazon.com](https://aws.amazon.com)
- Sign up for new account (gets free tier)
- Navigate to EC2 service

### 1.2 Launch Instance
1. Click **"Launch Instance"**
2. Choose **"Amazon Linux 2023"** (free tier eligible)
3. Select **"t2.micro"** (free tier eligible)
4. Create or select key pair (download .pem file)
5. Configure security group:
   - SSH (22) from your IP
   - HTTP (80) from anywhere
   - HTTPS (443) from anywhere
   - Custom TCP (8080) from anywhere

## Step 2: Connect to Instance

```bash
# Connect via SSH
ssh -i "your-key.pem" ec2-user@your-instance-ip

# Update system
sudo yum update -y

# Install Java 21
sudo yum install -y java-21-amazon-corretto

# Install Docker
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Logout and login again for Docker group
exit
ssh -i "your-key.pem" ec2-user@your-instance-ip
```

## Step 3: Deploy Your Application

```bash
# Clone your repository
git clone https://github.com/b-rine/ecommerce-stream.git
cd ecommerce-stream

# Build the application
./gradlew build -x test

# Run with Docker Compose (includes Kafka)
docker-compose up -d

# Or run directly with Java
java -jar build/libs/kafka-ecommerce-stream-0.0.1-SNAPSHOT.jar
```

## Step 4: Access Your Application

Your app will be available at:
- **HTTP**: `http://your-instance-ip:8080`
- **Dashboard**: `http://your-instance-ip:8080`
- **Logs**: `http://your-instance-ip:8080/logs`

## Step 5: Set Up Domain (Optional)

### 5.1 Get a Free Domain
- Use Freenom for free domains (.tk, .ml, .ga)
- Or use a cheap domain from Namecheap (~$1/month)

### 5.2 Configure Route 53 (AWS DNS)
```bash
1. Go to Route 53 in AWS Console
2. Create hosted zone for your domain
3. Update nameservers at your domain registrar
4. Create A record pointing to your EC2 IP
```

## Step 6: SSL Certificate (Optional)

```bash
# Install Certbot
sudo yum install -y certbot python3-certbot-nginx

# Get SSL certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

## Cost Breakdown

### AWS Free Tier (12 months):
```
✅ t2.micro instance: FREE
✅ 30 GB EBS storage: FREE  
✅ 15 GB bandwidth: FREE
✅ Elastic IP: FREE (when attached to running instance)

Total: $0/month for 12 months
```

### After Free Tier:
```
💰 t2.micro instance: ~$8.50/month
💰 30 GB storage: ~$3/month
💰 Data transfer: ~$0.09/GB
💰 Total: ~$12-15/month
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
```

### Logs
```bash
# Application logs
tail -f kafka-ecommerce.log

# System logs
sudo journalctl -u your-service
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

## Security Best Practices

1. **Keep system updated**: `sudo yum update -y`
2. **Use firewall**: Configure security groups properly
3. **Regular backups**: Backup your data directory
4. **Monitor access**: Check SSH access logs
5. **Use HTTPS**: Set up SSL certificate

## Troubleshooting

### Application won't start
```bash
# Check Java version
java -version

# Check port availability
netstat -tlnp | grep 8080

# Check logs
tail -f logs/kafka-ecommerce.log
```

### Out of memory
```bash
# Increase swap space
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

This setup gives you a production-ready deployment on AWS EC2, completely free for 12 months!
