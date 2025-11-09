// api/sendNotification.js
import admin from 'firebase-admin';

// Initialize Firebase Admin (only once)
if (!admin.apps.length) {
  try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    console.log('Firebase Admin initialized successfully');
  } catch (error) {
    console.error('Failed to initialize Firebase Admin:', error);
  }
}

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { 
      token, 
      title, 
      body, 
      email, 
      isSelfAlert,
      fullName,
      phoneNumber,
      lastKnownLatitude,
      lastKnownLongitude,
      frontPhotoUrl,
      backPhotoUrl,
      alertId  // ✅ ADD THIS - IMPORTANT!
    } = req.body;

    // Validate required fields
    if (!token || !title || !body || !email) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['token', 'title', 'body', 'email'],
        received: { 
          token: !!token, 
          title: !!title, 
          body: !!body, 
          email: !!email 
        }
      });
    }

    // LOG EVERYTHING for debugging
    console.log(`========================================`);
    console.log(`Sending notification to ${email}`);
    console.log(`Alert ID: ${alertId}`);  // ✅ ADD THIS LOG
    console.log(`Location received: lat=${lastKnownLatitude}, lon=${lastKnownLongitude}`);
    console.log(`Photo URLs: front=${frontPhotoUrl}, back=${backPhotoUrl}`);
    console.log(`Full request body:`, JSON.stringify(req.body, null, 2));
    console.log(`========================================`);

    // Convert location to string, handle null/undefined properly
    const latStr = (lastKnownLatitude !== null && lastKnownLatitude !== undefined) 
      ? String(lastKnownLatitude) 
      : '';
    const lonStr = (lastKnownLongitude !== null && lastKnownLongitude !== undefined) 
      ? String(lastKnownLongitude) 
      : '';
    const frontPhoto = frontPhotoUrl || '';
    const backPhoto = backPhotoUrl || '';

    console.log(`Converted strings: lat='${latStr}', lon='${lonStr}', front='${frontPhoto}', back='${backPhoto}'`);

    // Send FCM notification with ONLY data payload
    // This ensures our app code handles the notification in both open and closed states
    const message = {
      token: token,
      // REMOVED notification payload - only send data
      data: {
        title: title || '',
        body: body || '',
        email: email || '',
        fullName: fullName || '',
        phoneNumber: phoneNumber || '',
        lastKnownLatitude: latStr,
        lastKnownLongitude: lonStr,
        frontPhotoUrl: frontPhoto,
        backPhotoUrl: backPhoto,
        isSelfAlert: String(isSelfAlert || false),
        alertId: alertId || ''  // ✅ ADD THIS - CRITICAL FOR CONFIRMATION!
      },
      android: {
        priority: 'high'
      }
    };

    console.log('FCM message data being sent:', JSON.stringify(message.data, null, 2));

    const response = await admin.messaging().send(message);
    console.log('FCM notification sent successfully:', response);
    
    return res.status(200).json({ 
      success: true,
      messageId: response,
      recipient: email,
      alertId: alertId,  // ✅ ADD THIS TO RESPONSE
      locationSent: {
        latitude: latStr,
        longitude: lonStr
      },
      photosSent: {
        front: frontPhoto,
        back: backPhoto
      }
    });

  } catch (error) {
    console.error('Error sending notification:', error);
    return res.status(500).json({ 
      success: false,
      error: error.message || 'Failed to send notification',
      code: error.code
    });
  }
}

export const config = {
  api: {
    bodyParser: {
      sizeLimit: '1mb',
    },
  },
}
