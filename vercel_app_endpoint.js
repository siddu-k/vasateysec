const admin = require('firebase-admin');
const { createClient } = require('@supabase/supabase-js');

// Constants
const ANDROID_CHANNEL_ID = 'guardian_alert_channel';
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY;

// Initialize Firebase Admin SDK if not already initialized
if (!admin.apps.length) {
  try {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT))
    });
  } catch (error) {
    console.error('Firebase Admin Initialization Error:', error);
    throw new Error('Firebase configuration error');
  }
}

// Initialize Supabase client
const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY, {
  auth: {
    autoRefreshToken: false,
    persistSession: false
  }
});

// CORS headers for cross-origin requests
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Max-Age': '86400',
};

export default async function handler(req, res) {
  try {
    // Set CORS headers
    Object.entries(corsHeaders).forEach(([key, value]) => {
      res.setHeader(key, value);
    });

    // Handle preflight OPTIONS request
    if (req.method === 'OPTIONS') {
      return res.status(200).end();
    }

    // Only allow POST requests
    if (req.method !== 'POST') {
      return res.status(405).json({ 
        error: 'Method not allowed', 
        message: 'Only POST requests are supported' 
      });
    }

    // Validate request body exists and has required fields
    if (!req.body || typeof req.body !== 'object') {
      console.error('Invalid or missing request body');
      return res.status(400).json({
        error: 'Invalid request',
        message: 'Request body must be a valid JSON object'
      });
    }

    // Required fields
    const requiredFields = ['token', 'title', 'body', 'fullName', 'email'];
    const missingFields = requiredFields.filter(field => !req.body[field]);
    
    if (missingFields.length > 0) {
      console.error('Missing required fields:', missingFields);
      return res.status(400).json({
        error: 'Missing required fields',
        message: `The following fields are required: ${missingFields.join(', ')}`,
        missingFields: missingFields
      });
    }

    try {
      // Destructure all fields from the request body
      const { token, title, body, fullName, email, phoneNumber, lastKnownLatitude, lastKnownLongitude } = req.body;

      // Log incoming request for debugging
      console.log('Incoming request:', {
        hasToken: !!token,
        tokenPrefix: token ? token.substring(0, 20) + '...' : 'null',
        title: title,
        body: body,
        fullName: fullName,
        timestamp: new Date().toISOString()
      });

      if (!token) {
        console.error('Missing FCM token in request');
        return res.status(400).json({ 
          error: 'Missing required field', 
          message: 'FCM token is required' 
        });
      }

      if (!title || !body) {
        console.error('Missing title or body in request', { title: !!title, body: !!body });
        return res.status(400).json({ 
          error: 'Missing required fields', 
          message: 'Both title and body are required' 
        });
      }

      try {
        // Log the notification in Supabase first
        const { data: notificationData, error: notificationError } = await supabase
          .from('notifications')
          .insert([{
            recipient_email: email,
            title: title,
            body: body,
            fcm_token: token,
            status: 'sending',
            metadata: {
              fullName: fullName,
              phoneNumber: phoneNumber,
              lastKnownLatitude: lastKnownLatitude,
              lastKnownLongitude: lastKnownLongitude
            }
          }])
          .select();

        if (notificationError) {
          console.error('Error saving notification to Supabase:', notificationError);
          // Continue with sending the notification even if logging fails
        } else {
          console.log('Notification logged in Supabase with ID:', notificationData[0]?.id);
        }

        // Construct the FCM message - DATA ONLY (no notification object)
        const message = {
          token: token,
          data: {
            title: title,
            body: body,
            fullName: fullName || '',
            email: email || '',
            phoneNumber: phoneNumber || '',
            lastKnownLatitude: String(lastKnownLatitude || ''),
            lastKnownLongitude: String(lastKnownLongitude || ''),
            timestamp: new Date().toISOString(),
            source: 'vasatey-notify',
            alertType: 'emergency',
            channelId: ANDROID_CHANNEL_ID,
            notificationId: notificationData?.[0]?.id || ''
          },
          android: {
            priority: 'high',
          },
          apns: {
            payload: {
              aps: {
                'content-available': 1, // Silent notification - app handles display
              },
            },
            headers: {
              'apns-priority': '5', // Background priority for data-only
              'apns-push-type': 'background',
            },
          },
        };

        // Send the notification
        console.log('Sending FCM message to token:', token.substring(0, 20) + '...');
        const response = await admin.messaging().send(message);
        
        console.log('Successfully sent data message:', response);
        
        // Update notification status in Supabase
        if (notificationData?.[0]?.id) {
          const { error: updateError } = await supabase
            .from('notifications')
            .update({ 
              status: 'sent',
              sent_at: new Date().toISOString(),
              fcm_message_id: response
            })
            .eq('id', notificationData[0].id);

          if (updateError) {
            console.error('Error updating notification status in Supabase:', updateError);
          }
        }
        
        return res.status(200).json({
          success: true,
          message: 'Data message sent successfully',
          messageId: response,
          notificationId: notificationData?.[0]?.id,
          timestamp: new Date().toISOString(),
        });

      } catch (error) {
        // Update notification status to failed in Supabase
        if (notificationData?.[0]?.id) {
          await supabase
            .from('notifications')
            .update({ 
              status: 'failed',
              error_message: error.message,
              error_code: error.code
            })
            .eq('id', notificationData[0].id);
        }
        
        throw error; // Re-throw to be caught by the outer catch block
      }

    } catch (error) {
      const tokenPrefix = req.body?.token ? req.body.token.substring(0, 20) + '...' : 'null';
      console.error('Error sending message:', error);
      console.error('Full error details:', {
        code: error.code,
        message: error.message,
        token: tokenPrefix,
        timestamp: new Date().toISOString(),
        stack: error.stack
      message: error.message,
      token: tokenPrefix,
      timestamp: new Date().toISOString(),
      stack: error.stack
    });
    
    // Handle specific Firebase errors
    if (error.code === 'messaging/registration-token-not-registered') {
      console.log(`Token not registered: ${tokenPrefix}`);
      return res.status(410).json({
        error: 'Token expired',
        message: 'The FCM token is not registered or has expired. Please refresh your app to get a new token.',
        code: error.code,
        action: 'refresh_token',
        details: 'This token has been invalidated and should be refreshed by the app'
      });
    }
    
    if (error.code === 'messaging/invalid-registration-token') {
      console.log(`Invalid token format: ${tokenPrefix}`);
      return res.status(400).json({
        error: 'Invalid token format',
        message: 'The FCM token format is invalid. Please refresh your app to get a valid token.',
        code: error.code,
        action: 'refresh_token',
        details: 'Token format is malformed'
      });
    }
    
    if (error.code === 'messaging/invalid-argument') {
      return res.status(400).json({
        error: 'Invalid argument',
        message: 'One or more arguments to the request are invalid',
        code: error.code,
        details: error.message,
      });
    }

    if (error.code === 'messaging/message-rate-exceeded') {
      return res.status(429).json({
        error: 'Rate limit exceeded',
        message: 'Message rate exceeded for this token',
        code: error.code,
        action: 'retry_later',
      });
    }

    // Generic error response
    return res.status(500).json({
      error: 'Internal server error',
      message: 'Failed to send notification',
      code: error.code || 'unknown',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined,
    });
  }

  } catch (outerError) {
    console.error('Critical function error:', outerError);
    return res.status(500).json({
      error: 'Function execution failed',
      message: 'A critical error occurred while processing the request',
      details: process.env.NODE_ENV === 'development' ? outerError.message : undefined,
    });
  }
}