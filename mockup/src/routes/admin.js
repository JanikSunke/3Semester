const express = require('express');
const axios = require('axios');
const config = require('./../config.json');

const User = require('./../classes/User');
const pathPrefix = 'admin';

const router = express.Router();

router.get('/invalidpermission', function(req, res, next) {
  res.render(
    `${pathPrefix}/invalidPermission`,
    {
      title: '[ADMIN] Invalid Permission'
    });
});

router.get('/index', function(req, res, next) {
  res.redirect('/notsignedin');
});

router.post('/index', function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;
    if(`${req.body.userId}` == config.admin_user_id) {
      res.render(
        `${pathPrefix}/index`,
        {
          title: '[ADMIN] mockup',
          pathPrefix: pathPrefix,
          active: { adminIndex: true },
          user: user
        });
    } else {
      res.redirect('invalidpermission');
    }
  } else {
    res.redirect('/notsignedin');
  }
});

router.get('/cashflow', function(req, res, next) {
  res.redirect(`../notsignedin`);
});

router.post('/cashflow', async function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;
    if(`${req.body.userId}` == config.admin_user_id) {
      const cashflow = await axios.get(`${config.api_url}/cashflow/`, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res.data[0];
      }).catch((err) => {
        console.log(err);
      });

      res.render(
        `${pathPrefix}/cashflow`,
        {
          title: '[ADMIN] Cashflow',
          active: { adminCashflow: true },
          user: user,
          cashflow: cashflow
        });
    } else {
      res.redirect('invalidpermission');
    }
  } else {
    res.redirect('/notsignedin');
  }
});


router.get('/invoices', function(req, res, next) {
  res.redirect(`../notsignedin`);
});

router.post('/invoices', async function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;
    if(`${req.body.userId}` == config.admin_user_id) {
      const invoices = await axios.get(`${config.api_url}/invoice/list?all&user=${user.id}`, {
        headers: {
          "x-user-id": user.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res.data;
      }).catch((err) => {
        console.log(err);
      });

      if(invoices) {
        for (let i = 0; i < invoices.length; i++) {
          invoices[i].totalPrice = invoices[i].totalPrice / 100;
          const renewsAt = new Date(invoices[i].date * 1000);
          invoices[i].date = `${renewsAt.getDate()}/${renewsAt.getMonth() + 1}-${renewsAt.getFullYear()}`;
        }
      }

      res.render(
        `${pathPrefix}/invoices`,
        {
          title: '[ADMIN] Users',
          pathPrefix: pathPrefix,
          active: { adminInvoices: true },
          id: req.body.id,
          user: user,
          invoices: invoices
        });
    } else {
      res.redirect('invalidpermission');
    }
  } else {
    res.redirect('/notsignedin');
  }
});

router.get('/users/profile', function(req, res, next) {
  res.redirect(`../notsignedin`);
});

router.post('/users/profile', async function(req, res, next) {
  if(req.body.userId) {
    const user = new User();
    user.id = req.body.userId;

    const viewedUser = new User();
    viewedUser.id = req.body.invoiceUserId;

    if(`${req.body.userId}` == config.admin_user_id) {
      viewedUser.subscription = await axios.get(`${config.api_url}/subscription/user?user=${viewedUser.id}`, {
        headers: {
          "x-user-id": viewedUser.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res.data;
      }).catch((err) => {
        console.log(err);
      });

      viewedUser.invoices = await axios.get(`${config.api_url}/invoice/list?user=${viewedUser.id}`, {
        headers: {
          "x-user-id": viewedUser.id,
          "x-test-db": config.xtestdb
        }
      }).then((res) => {
        return res.data;
      }).catch((err) => {
        console.log(err);
      });

      if(viewedUser.subscription) {
        for (let i = 0; i < viewedUser.subscription.subscriptions.length; i++) {
          viewedUser.subscription.subscriptions[i].price = viewedUser.subscription.subscriptions[i].price / 100;
          const renewsAt = new Date(viewedUser.subscription.subscriptions[i].renewsAt * 1000);
          viewedUser.subscription.subscriptions[i].renewsAt = `${renewsAt.getDate()}/${renewsAt.getMonth() + 1}-${renewsAt.getFullYear()}`;
        }
      }

      if(viewedUser.invoices) {
        for (let i = 0; i < viewedUser.invoices.length; i++) {
          viewedUser.invoices[i].totalPrice = viewedUser.invoices[i].totalPrice / 100;
          const renewsAt = new Date(viewedUser.invoices[i].date * 1000);
          viewedUser.invoices[i].date = `${renewsAt.getDate()}/${renewsAt.getMonth() + 1}-${renewsAt.getFullYear()}`;
        }
      }

      res.render(
        `${pathPrefix}/usersProfile`,
        {
          title: '[ADMIN] Users Profile',
          active: { adminUsersProfile: true },
          user: user,
          viewedUser: viewedUser
        });
    } else {
      res.redirect('invalidpermission');
    }
  } else {
    res.redirect('/notsignedin');
  }
});

module.exports = router;
