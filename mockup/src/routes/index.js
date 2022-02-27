const express = require('express');
const axios = require('axios');
const config = require('./../config.json');

const User = require('./../classes/User');

const router = express.Router();

router.get('/', function(req, res, next) {
  res.render(
    'index',{
      title: 'mockup',
      active: {home: true}
    });
});

router.post('/', function(req, res, next) {
  res.render(
    'index', {
      title: 'mockup',
      active: {home: true},
      user: { id: req.body.userId }
    });
});

router.get('/notsignedin', function(req, res, next) {
  res.render(
    `notSignedIn`, {
      title: '[USER] Not signed in'
    });
});

router.get('/signin', function(req, res, next) {
  res.render('signIn', { title: 'sign in', active: {signIn: true} });
});

router.post('/signin', function(req, res, next) {
  res.redirect('/signIn');
});

router.get('/profile', function(req, res, next) {
  res.redirect('/notSignedIn');
});

router.post('/profile', async function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;

    if(req.body.subscriptionId) {
      var prefix = ""
      if (req.body.force) {
        prefix = "/force"
      }
      await axios.delete(
        `${config.api_url}/subscription/${req.body.subscriptionId}${prefix}?user=${user.id}`,
        {
          headers: {
            "x-user-id": user.id,
            "x-test-db": config.xtestdb
          }
        }
      ).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    if(req.body.subscriptionTier) {
      await axios.post(
        `${config.api_url}/subscription/create?user=${user.id}`,
        {
          subscription: req.body.subscriptionTier
        },
        {
          headers: {
            "x-user-id": user.id,
            "x-test-db": config.xtestdb
          }
        }
      ).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };
    
    if(req.body.paymentId) {
      await axios.delete(`${config.api_url}/payments/${req.body.paymentId}?user=${user.id}`, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    if(req.body.cardNumber) {
      await axios.post(`${config.api_url}/payments/?user=${user.id}`, {
        "cardNumber": req.body.cardNumber,
        "expireMonth": req.body.expireMonth,
        "expireYear": req.body.expireYear,
        "cvc": req.body.cvc
      }, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    if(req.body.familyLeave) {
      await axios.delete(`${config.api_url}/family/?user=${user.id}`, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    if(req.body.familyCreate) {
      await axios.post(`${config.api_url}/family/?user=${user.id}`, {}, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    if(req.body.familyId) {
      await axios.post(`${config.api_url}/family/${req.body.familyId}?user=${user.id}`, {}, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res;
      }).catch((err) => {
        console.log(err);
      });
    };

    user.subscription = await axios.get(`${config.api_url}/subscription/user?user=${user.id}`, {
      headers: {
        "x-user-id": user.id,
        "x-test-db": config.xtestdb
      }
    }).then((res) => {
      return res.data;
    }).catch((err) => {
      console.log(err);
      return null;
    });

    if(user.subscription) {
      for (let i = 0; i < user.subscription.subscriptions.length; i++) {
        user.subscription.subscriptions[i].price = user.subscription.subscriptions[i].price / 100;
        const renewsAt = new Date(user.subscription.subscriptions[i].renewsAt * 1000);
        user.subscription.subscriptions[i].renewsAt = `${renewsAt.getDate()}/${renewsAt.getMonth() + 1}-${renewsAt.getFullYear()}`;
      }
    }

    subscriptionTiers = await axios.get(`${config.api_url}/subscription/list?user=${user.id}`, {
      headers: {
        "x-user-id": user.id,
        "x-test-db": config.xtestdb
      }
    }).then((res) => {
      return res.data;
    }).catch((err) => {
      console.log(err);
    });

    if(subscriptionTiers) {
      for (let i = 0; i < subscriptionTiers.length; i++) {
        subscriptionTiers[i].price = subscriptionTiers[i].price / 100;
      }
    }

    user.paymentMethods = await axios.get(`${config.api_url}/payments/get?user=${user.id}`, {
      headers: {
        "x-user-id": user.id,
        "x-test-db": config.xtestdb
      }
    }).then((res) => {
      return res.data;
    }).catch((err) => {
      console.log(err);
    });

    user.family = await axios.get(`${config.api_url}/family/user?user=${user.id}`, {
      headers: {
        "x-user-id": user.id,
        "x-test-db": config.xtestdb
      }
    }).then((res) => {
      return res.data;
    }).catch((err) => {
      console.log(err);
      return false
    });

    if(user.subscription && user.subscription.effectiveTier == "FAMILY") {
      let prefix = "to"

      if(user.subscription.premiumFromFamily) {
        prefix = "from"
      }

      prefix = user.family && user.family.isInFamily ? `${prefix} ${user.family.familyId}` : `${prefix} only you!`;

      user.subscription.effectiveTier = `${user.subscription.effectiveTier} ${prefix}`;
    }

    res.render(
      'profile',
      {
        title: '[USER] Profile',
        active: {users: true},
        user: user,
        subscriptionTiers: subscriptionTiers
      });
  } else {
    res.redirect('/notSignedIn');
  }
});

router.get('/invoices', function(req, res, next) {
  res.redirect('/notSignedIn');
});

router.post('/invoices', async function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;
    user.invoices = await axios.get(`${config.api_url}/invoice/list?user=${user.id}`, {
      headers: {
        "x-user-id": user.id,
        "x-test-db": config.xtestdb
      }
    }).then((res) => {
      return res.data;
    }).catch((err) => {
      console.log(err);
    });

    if(user.invoices) {
      for (let i = 0; i < user.invoices.length; i++) {
        user.invoices[i].totalPrice = user.invoices[i].totalPrice / 100;
        const renewsAt = new Date(user.invoices[i].date * 1000);
        user.invoices[i].date = `${renewsAt.getDate()}/${renewsAt.getMonth() + 1}-${renewsAt.getFullYear()}`;
      }
    }

    res.render(
      'invoices',
      {
        title: 'Invoices',
        active: {invoices: true},
        user: user,
        api: `${config.api_url}`
      });
  } else {
    res.redirect('/notSignedIn');
  }
});

router.get('/invoice/:invoiceId/:userId', async function(req, res, next) {
  res.redirect(`${config.api_url}/invoice/${req.params.invoiceId}/view?user=${req.params.userId}`);
});

module.exports = router;
