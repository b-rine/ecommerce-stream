# Railway Deployment Guide

## Quick Setup (5 minutes)

### 1. Sign up for Railway
- Go to [railway.app](https://railway.app)
- Sign up with your GitHub account
- This gives you automatic deployments from your repo

### 2. Deploy from GitHub
1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Choose your `ecommerce-stream` repository
4. Railway will automatically detect it's a Java/Spring Boot app

### 3. Configure Environment Variables
In your Railway project dashboard, go to **Variables** and add:

```
SPRING_PROFILES_ACTIVE=production
DATABASE_PASSWORD=your-secure-password
```

### 4. Add PostgreSQL Database (Optional)
For production, you might want a real database:
1. In Railway dashboard, click **"+ New"**
2. Select **"Database"** → **"PostgreSQL"**
3. Railway will provide connection strings automatically

### 5. Deploy!
- Railway automatically builds and deploys your app
- Your app will be available at: `https://your-app-name.railway.app`

## What Railway Does Automatically

✅ **Detects Java 21** from your build files  
✅ **Builds with Gradle** using `./gradlew build`  
✅ **Runs health checks** at `/actuator/health`  
✅ **Handles port binding** (Railway provides PORT env var)  
✅ **Auto-restarts** on crashes  
✅ **Provides HTTPS** with custom domain support  

## Monitoring Your App

- **Logs**: View real-time logs in Railway dashboard
- **Metrics**: CPU, memory, and request metrics
- **Health**: Automatic health checks and uptime monitoring

## Custom Domain (Optional)

1. In Railway dashboard → **Settings** → **Domains**
2. Add your custom domain
3. Railway provides SSL certificate automatically

## Troubleshooting

### Build Fails
- Check logs in Railway dashboard
- Ensure `railway.toml` is in root directory
- Verify Java 21 compatibility

### App Won't Start
- Check environment variables
- Verify health check endpoint (`/actuator/health`)
- Check application logs

### Database Issues
- H2 file database works in Railway
- For production, consider PostgreSQL addon

## Cost

- **Free tier**: $5/month in Railway credits (usually enough for small apps)
- **Pro tier**: $20/month for higher limits
- **Usage-based**: Pay for what you use

Your app should deploy successfully with the provided configuration!
